/*
 * (c) Rob Gordon 2006
 */
package org.oddjob.jmx.handlers;

import org.junit.Test;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.oddjob.OjTestCase;

import org.oddjob.MockStateful;
import org.oddjob.Stateful;
import org.oddjob.jmx.RemoteOperation;
import org.oddjob.jmx.client.MockClientSideToolkit;
import org.oddjob.jmx.server.MockServerSideToolkit;
import org.oddjob.jmx.server.ServerInterfaceHandler;
import org.oddjob.state.JobState;
import org.oddjob.state.StateEvent;
import org.oddjob.state.StateListener;

public class StatefulHandlerFactoryTest extends OjTestCase {

	private class OurStateful extends MockStateful {
		StateListener l;
		public void addStateListener(StateListener listener) {
			assertNull(l);
			l = listener;
			l.jobStateChange(new StateEvent(this, JobState.READY));
		}
		public void removeStateListener(StateListener listener) {
			assertNotNull(l);
			l = null;
		}		
	}
	
	private class OurClientToolkit extends MockClientSideToolkit {
		ServerInterfaceHandler server;

		NotificationListener listener;
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T invoke(RemoteOperation<T> remoteOperation, Object... args)
				throws Throwable {
			return (T) server.invoke(remoteOperation, args);
		}
		
		public void registerNotificationListener(String eventType, NotificationListener notificationListener) {
			if (listener != null) {
				throw new RuntimeException("Only one listener expected.");
			}
			assertEquals(StatefulHandlerFactory.STATE_CHANGE_NOTIF_TYPE, eventType);
			
			this.listener = notificationListener;
		}
		
		@Override
		public void removeNotificationListener(String eventType,
				NotificationListener notificationListener) {
			if (listener == null) {
				throw new RuntimeException("Only one listener remove expected.");
			}
			
			assertEquals(StatefulHandlerFactory.STATE_CHANGE_NOTIF_TYPE, eventType);
			assertEquals(this.listener, notificationListener);
			
			this.listener = null;
		}
	}

	private class OurServerSideToolkit extends MockServerSideToolkit {

		long seq = 0;
		
		NotificationListener listener;
		
		public void runSynchronized(Runnable runnable) {
			runnable.run();
		}
		
		@Override
		public Notification createNotification(String type) {
			return new Notification(type, this, seq++);
		}
		
		public void sendNotification(Notification notification) {
			if (listener != null) {
				listener.handleNotification(notification, null);
			}
		}
				
	}
	
	private class Result implements StateListener {
		StateEvent event;
		
		public void jobStateChange(StateEvent event) {
			this.event = event;
		}
	}
	
   @Test
	public void testAddRemoveListener() throws Exception {
		
		StatefulHandlerFactory test = new StatefulHandlerFactory();
		
		assertEquals(1, test.getMBeanNotificationInfo().length);
		
		OurStateful stateful = new OurStateful(); 
		OurServerSideToolkit serverToolkit = new OurServerSideToolkit();

		// create the handler
		ServerInterfaceHandler serverHandler = test.createServerHandler(
				stateful, serverToolkit);

		// which should add a listener to our stateful
		assertNotNull("listener added.", stateful.l);

		OurClientToolkit clientToolkit = new OurClientToolkit();

		Stateful local = new StatefulHandlerFactory.ClientStatefulHandlerFactory(
				).createClientHandler(new MockStateful(), clientToolkit);
		
		clientToolkit.server = serverHandler;

		Result result = new Result();
		
		local.addStateListener(result);
		
		assertEquals("State ready", JobState.READY, 
				result.event.getState());

		Result result2 = new Result();
		
		local.addStateListener(result2);
		
		assertEquals("State ready", JobState.READY, 
				result2.event.getState());

		serverToolkit.listener = clientToolkit.listener;
		
		stateful.l.jobStateChange(new StateEvent(stateful, JobState.COMPLETE));

		// check the notification is sent
		assertEquals("State complete", JobState.COMPLETE, 
				result.event.getState());
		assertEquals("State complete", JobState.COMPLETE, 
				result2.event.getState());
		
		local.removeStateListener(result);
		
		assertNotNull(clientToolkit.listener);
		
		local.removeStateListener(result2);
		
		assertNull(clientToolkit.listener);
		
	}
		
	private class OurClientToolkit2 extends MockClientSideToolkit {
		ServerInterfaceHandler server;

		@SuppressWarnings("unchecked")
		@Override
		public <T> T invoke(RemoteOperation<T> remoteOperation, Object... args)
				throws Throwable {
			return (T) server.invoke(remoteOperation, args);
		}
	}
	
   @Test
	public void testLastStateEventCallsRemoteOpWhenNoListenerAdded() throws Exception {
		
		StatefulHandlerFactory test = new StatefulHandlerFactory();
		
//		assertEquals(1, test.getMBeanOperationInfo().length);
		
		OurStateful stateful = new OurStateful(); 
		OurServerSideToolkit serverToolkit = new OurServerSideToolkit();

		// create the handler
		ServerInterfaceHandler serverHandler = test.createServerHandler(
				stateful, serverToolkit);

		OurClientToolkit2 clientToolkit = new OurClientToolkit2();

		Stateful local = new StatefulHandlerFactory.ClientStatefulHandlerFactory(
				).createClientHandler(new MockStateful(), clientToolkit);
		
		clientToolkit.server = serverHandler;
		
		stateful.l.jobStateChange(new StateEvent(stateful, JobState.COMPLETE));
		
		StateEvent lastStateEvent = local.lastStateEvent();
		
		assertEquals(JobState.COMPLETE, 
				lastStateEvent.getState());
		
	}
	
	
}
