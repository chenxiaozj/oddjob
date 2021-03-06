package org.oddjob.monitor.action;

import org.junit.Test;

import org.oddjob.OjTestCase;

import org.oddjob.Loadable;
import org.oddjob.monitor.model.MockExplorerContext;
import org.oddjob.util.MockThreadManager;
import org.oddjob.util.ThreadManager;

public class LoadActionTest extends OjTestCase {

	private class OurLoadable implements Loadable {
		
		boolean loadable = true;
		
		public boolean isLoadable() {
			return loadable;
		}
		
		public void load() {
			setLoadable(false);
		}
		
		@Override
		public void unload() {
			throw new RuntimeException("Unexpected.");
		}
		
		void setLoadable(boolean loadable) {
			this.loadable = loadable;
		}
	}
	
	class OurEContext extends MockExplorerContext {
		
		OurLoadable loadable = new OurLoadable();
		
		@Override
		public Object getThisComponent() {
			return loadable;
		}
		
		@Override
		public ThreadManager getThreadManager() {
			return new MockThreadManager() {
				@Override
				public void run(Runnable runnable, String description) {
					runnable.run();
				}
			};
		}
		
	}
	
   @Test
	public void testCycle() throws Exception {
		
		LoadAction test = new LoadAction();
		assertFalse(test.isEnabled());
		assertFalse(test.isVisible());
		
		OurEContext eContext = new OurEContext();
		
		test.setSelectedContext(eContext);
		test.prepare();
		
		assertTrue(test.isEnabled());
		assertTrue(test.isVisible());
		
		assertTrue(eContext.loadable.loadable);
		
		test.action();
		
		test.prepare();
		
		assertFalse(eContext.loadable.loadable);
		
		assertFalse(test.isEnabled());
		assertTrue(test.isVisible());
		
		test.setSelectedContext(null);
		
		assertFalse(test.isEnabled());
		assertFalse(test.isVisible());
	}
	
}
