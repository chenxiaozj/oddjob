/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.monitor.control;

import org.junit.Test;

import java.util.Map;

import org.oddjob.OjTestCase;

import org.oddjob.Stateful;
import org.oddjob.arooa.ArooaSession;
import org.oddjob.arooa.standard.StandardArooaSession;
import org.oddjob.monitor.model.DetailModel;
import org.oddjob.monitor.model.MockExplorerContext;
import org.oddjob.monitor.model.PropertyModel;
import org.oddjob.state.JobState;
import org.oddjob.state.StateEvent;
import org.oddjob.state.StateListener;

public class PropertyPollingTest extends OjTestCase {

	private class OurExplorerContext extends MockExplorerContext {
		@Override
		public Object getThisComponent() {
			return new Comp();
		}
	}

	public static class Comp {
		public String getFruit() {
			return "apples";
		}
	}
	
	/**
	 * Test what happens when the property tab
	 * is selected.
	 *
	 */
   @Test
	public void testSelected() {
		
		ArooaSession session = new StandardArooaSession();
		
		PropertyModel model = new PropertyModel();
		
		PropertyPolling test = new PropertyPolling(this, session);
		test.setPropertyModel(model);

		DetailModel detailModel = new DetailModel();
		
		detailModel.addPropertyChangeListener(test);
		
		detailModel.setTabSelected(DetailModel.PROPERTIES_TAB);

		OurExplorerContext ec = new OurExplorerContext();
		
		detailModel.setSelectedContext(ec);
		
		test.poll();
		
		String result = (String) model.getProperties().get("fruit");
		assertEquals("apples", result);
	}
	
	/**
	 * Test what happens when the property tab
	 * is unselected.
	 *
	 */
   @Test
	public void testNotSelected() {
		
		ArooaSession session = new StandardArooaSession();
		
		PropertyModel model = new PropertyModel();
		
		PropertyPolling test = new PropertyPolling(this, session);
		test.setPropertyModel(model);

		DetailModel detailModel = new DetailModel();
		detailModel.addPropertyChangeListener(
				test);
		
		OurExplorerContext ec = new OurExplorerContext();
		
		detailModel.setSelectedContext(ec);
		
		test.poll();
		
		Map<String, String> props = model.getProperties();
		assertEquals(0, props.size());
	}
	
	public class OurStateful implements Stateful {
		
		private StateListener listener;
		
		@Override
		public void addStateListener(StateListener listener) {
			assertNotNull(listener);
			assertNull(this.listener);
			this.listener = listener;
		}
		
		@Override
		public StateEvent lastStateEvent() {
			throw new RuntimeException("Unexpected.");
		}
		
		@Override
		public void removeStateListener(StateListener listener) {
			assertNotNull(listener);
			assertSame(this.listener, listener);
			this.listener = null;
		}
	}
	
	private class OurExplorerContext2 extends MockExplorerContext {
		
		OurStateful stateful = new OurStateful();
		
		@Override
		public Object getThisComponent() {
			return stateful;
		}
	}

   @Test
	public void testSelectedStateful() {
		
		ArooaSession session = new StandardArooaSession();
		
		PropertyModel model = new PropertyModel();
		
		PropertyPolling test = new PropertyPolling(this, session);
		test.setPropertyModel(model);

		DetailModel detailModel = new DetailModel();
		
		detailModel.addPropertyChangeListener(test);
		
		detailModel.setTabSelected(DetailModel.PROPERTIES_TAB);

		OurExplorerContext2 ec = new OurExplorerContext2();
		
		detailModel.setSelectedContext(ec);

		assertNotNull(ec.stateful.listener); 
		
		ec.stateful.listener.jobStateChange(
				new StateEvent(ec.stateful, JobState.COMPLETE)); 
		
		detailModel.setSelectedContext(null);
		
		assertNull(ec.stateful.listener); 
		
	}
}
