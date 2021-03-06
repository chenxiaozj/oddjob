package org.oddjob.jmx;
import org.junit.Before;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.rmi.RMIConnectorServer;

import org.oddjob.OjTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.oddjob.OddjobConsole;
import org.oddjob.Stateful;
import org.oddjob.arooa.standard.StandardArooaSession;
import org.oddjob.state.FlagState;
import org.oddjob.state.JobState;
import org.oddjob.state.ServiceState;
import org.oddjob.tools.OddjobTestHelper;
import org.oddjob.tools.StateSteps;

public class NetworkFailureTest extends OjTestCase {

	private static final Logger logger = LoggerFactory.getLogger(NetworkFailureTest.class);
	
   @Before
   public void setUp() throws Exception {

		logger.info("------------------- " + getName() + " --------------");
	}
	
   @Test
	public void testSimpleExample() throws Exception {
	
		try (OddjobConsole.Close close = OddjobConsole.initialise()) {
			
			FlagState root = new FlagState();
			root.setName("Our Job");
			
			Map<String, Object> env = new HashMap<String, Object>();
	
			FailableSocketFactory ssf =  
				new FailableSocketFactory(); 
			
			env.put(RMIConnectorServer. 
					RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,ssf); 
	
			JMXServerJob server = new JMXServerJob();
			server.setRoot(root);
			server.setArooaSession(new StandardArooaSession());
			server.setUrl("service:jmx:rmi://");
			server.setEnvironment(env);
			
			server.start();
			
			JMXClientJob client = new JMXClientJob();
			client.setConnection(server.getAddress());
			client.setArooaSession(new StandardArooaSession());
			client.setHeartbeat(500);
			
			StateSteps clientStates = new StateSteps(client);
			clientStates.startCheck(ServiceState.STARTABLE, 
					ServiceState.STARTING, ServiceState.STARTED);
			
			client.run();
			
			clientStates.checkNow();
			
			Object[] children = OddjobTestHelper.getChildren(client);
			
			assertEquals(1, children.length);
			
			Stateful child = (Stateful) children[0];
			
			assertEquals("Our Job", child.toString()); 
			
			clientStates.startCheck(ServiceState.STARTED, 
					ServiceState.EXCEPTION);
			
			ssf.setFail(true);
			
			logger.debug("Server Job Running.");
			
			root.run();
			
			clientStates.checkWait();
					
			ssf.setFail(false);
			
			clientStates.startCheck(ServiceState.EXCEPTION, 
					ServiceState.STARTABLE, 
					ServiceState.STARTING,
					ServiceState.STARTED);
			
			logger.debug("Client Running Again.");
			
			client.hardReset();
			
			client.run();
			
			clientStates.checkNow();
			
			children = OddjobTestHelper.getChildren(client);
			
			assertEquals(1, children.length);
			
			child = (Stateful) children[0];
			
			assertEquals(JobState.COMPLETE, OddjobTestHelper.getJobState(child));
			assertEquals("Our Job", child.toString()); 
			
			client.stop();
			
			server.stop();
		}
	}
	
	
}
