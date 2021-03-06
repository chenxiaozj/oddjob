package org.oddjob;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import org.oddjob.OjTestCase;

import org.oddjob.arooa.MockArooaSession;
import org.oddjob.framework.Service;
import org.oddjob.tools.OddjobTestHelper;

public class OddjobComponentResolverTest extends OjTestCase {
	
   @Test
	public void testRunnable() {
		
		OddjobComponentResolver test = 
			new OddjobComponentResolver();
		
		Runnable runnable = new Runnable() {
			public void run() {
			}
		};
		
		Object proxy = test.resolve(runnable, new MockArooaSession());

		assertTrue(proxy instanceof Runnable);
	}
	
	public static class OurService implements Service {
		
		public void start() {}
		
		public void stop() {}
	}
	
   @Test
	public void testService() {
		
		OddjobComponentResolver test = 
			new OddjobComponentResolver();
				
		Object proxy = test.resolve(new OurService(), 
				new MockArooaSession());

		assertTrue(proxy instanceof Runnable);
	}
	
	static class OurSerializableRunnable implements Runnable, Serializable {
		private static final long serialVersionUID = 2009011000L;

		String colour="red";
		
		public void run() {
		}
	}
	
   @Test
	public void testRestore() throws IOException, ClassNotFoundException {
		
		OddjobComponentResolver test = 
			new OddjobComponentResolver();
		
		Object job = new OurSerializableRunnable();
		
		Object proxy = test.resolve(job, new MockArooaSession());

		Object restoredProxy = OddjobTestHelper.copy(proxy);
		
		Object restoredJob = test.restore(restoredProxy, 
				new MockArooaSession());
		
		assertEquals(OurSerializableRunnable.class, restoredJob.getClass());
		
		assertEquals(((OurSerializableRunnable) job).colour, "red");
	}
}
