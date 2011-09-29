/*
 * Copyright (c) 2005, Rob Gordon.
 */
package org.oddjob.jobs.structural;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.oddjob.ConsoleCapture;
import org.oddjob.FailedToStopException;
import org.oddjob.Helper;
import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.OddjobSessionFactory;
import org.oddjob.OurDirs;
import org.oddjob.Stateful;
import org.oddjob.Structural;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.ArooaSession;
import org.oddjob.arooa.ComponentTrinity;
import org.oddjob.arooa.convert.ArooaConversionException;
import org.oddjob.arooa.deploy.annotations.ArooaAttribute;
import org.oddjob.arooa.parsing.MockArooaContext;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.registry.BeanRegistry;
import org.oddjob.arooa.registry.ComponentPool;
import org.oddjob.arooa.registry.Path;
import org.oddjob.arooa.runtime.MockRuntimeConfiguration;
import org.oddjob.arooa.runtime.RuntimeConfiguration;
import org.oddjob.arooa.standard.StandardArooaSession;
import org.oddjob.arooa.types.XMLConfigurationType;
import org.oddjob.arooa.utils.DateHelper;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.framework.SimpleJob;
import org.oddjob.persist.MockPersisterBase;
import org.oddjob.state.FlagState;
import org.oddjob.state.JobState;
import org.oddjob.state.ParentState;
import org.oddjob.state.StateEvent;
import org.oddjob.state.StateListener;
import org.oddjob.structural.StructuralEvent;
import org.oddjob.structural.StructuralListener;

/**
 *
 * @author Rob Gordon.
 */
public class ForEachJobTest extends TestCase {
	private static final Logger logger = Logger.getLogger(ForEachJobTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		logger.info("--------------------  " + getName() + "  ---------------");
	}
	
	public static class OurJob extends SimpleJob {
		
		Object stuff;
		int index;
		boolean ran;
		
		@Override
		protected int execute() throws Throwable {
			ran = true;
			return 0;
		}
		
		@ArooaAttribute
		public void setStuff(Object stuff) {
			this.stuff = stuff;
		}

		public void setIndex(int index) {
			this.index = index;
		}
	}
	
	private class ChildCatcher implements StructuralListener {
		final List<Object> children = new ArrayList<Object>();
		
		public void childAdded(StructuralEvent event) {
			children.add(event.getIndex(), event.getChild());
		}
		public void childRemoved(StructuralEvent event) {
			children.remove(event.getIndex());
		}

	}
	
	public void testOneJobTwoValues() throws ArooaParseException {
		
		String xml = 
			"<foreach id='foreach'>" +
			" <job>" +
			"  <bean class='" + OurJob.class.getName() + 
			"' stuff='${foreach.current}' index='${foreach.index}'/>" +
			" </job>" +
			"</foreach>";
		
		ForEachJob test = new ForEachJob();
		test.setArooaSession(new OddjobSessionFactory().createSession());
		test.setConfiguration(new XMLConfiguration("XML", xml));
		test.setValues(Arrays.asList("apple", "orange"));
		
		ChildCatcher children = new ChildCatcher();
		
		test.addStructuralListener(children);
			
		test.run();
		
		assertEquals(2, children.children.size());
		
		OurJob job1 = (OurJob) children.children.get(0);
		OurJob job2 = (OurJob) children.children.get(1);
		
		assertEquals("apple", job1.stuff);
		assertEquals(0, job1.index);
		assertTrue(job1.ran);
		
		assertEquals("orange", job2.stuff);
		assertEquals(1, job2.index);
		assertTrue(job2.ran);
		
	}
	
	public void testLoadOnJobTwoValues() throws ArooaParseException {
		
		String xml = 
			"<foreach id='foreach'>" +
			" <job>" +
			"<bean class='" + OurJob.class.getName() + 
			"' stuff='${foreach.current}' index='${foreach.index}'/>" +
			" </job>" +
			"</foreach>";
		
		ForEachJob test = new ForEachJob();
		test.setArooaSession(new OddjobSessionFactory().createSession());
		test.setConfiguration(new XMLConfiguration("XML", xml));
		test.setValues(Arrays.asList("apple", "orange"));
		
		ChildCatcher children = new ChildCatcher();
		
		test.addStructuralListener(children);
			
		assertTrue(test.isLoadable());
		
		test.load();
		
		assertFalse(test.isLoadable());
		
		assertEquals(2, children.children.size());
		
		OurJob job1 = (OurJob) children.children.get(0);
		OurJob job2 = (OurJob) children.children.get(1);
		
		assertEquals("apple", job1.stuff);
		assertEquals(0, job1.index);
		assertFalse(job1.ran);
		
		assertEquals("orange", job2.stuff);
		assertEquals(1, job2.index);
		assertFalse(job2.ran);
		
	}
	
	
	
	public static class RegistryCheck extends SimpleJob {
		ArooaSession session;
		
		protected int execute() throws Throwable {
			session = getArooaSession();
			return 0;
		}
	}
	
	private class OurContext extends MockArooaContext {
		
		ArooaSession session;
		
		OurContext(ArooaSession session) {
			this.session = session;
		}
		
		@Override
		public ArooaSession getSession() {
			return session;
		}
		
	}
	
	public void testPseudoRegistry() {
		
		String findMe = new String("Fruit is Healthy.");
		
		StandardArooaSession session = new StandardArooaSession();
		session.getBeanRegistry().register(
				"fruit", findMe);

		String xml = 
			"<foreach id='test'>" +
			" <job>" +
			"  <bean class='" + RegistryCheck.class.getName() + "'/>" +
			" </job>" +
			"</foreach>";
		
		ForEachJob test = new ForEachJob();
		test.setValues(Arrays.asList("one"));
		test.setArooaSession(session);
		test.setConfiguration(new XMLConfiguration("XML", xml));
		
		// so test can configure itself when run.
		ComponentPool pool = session.getComponentPool();
		pool.registerComponent(
				new ComponentTrinity(test, test,
						new OurContext(session) {
					@Override
					public RuntimeConfiguration getRuntime() {
						return new MockRuntimeConfiguration() {
							@Override
							public void configure() {
							}
						};
					}
				}), 
				"test");
		
		test.run();
		
		ChildCatcher child = new ChildCatcher();
		test.addStructuralListener(child);
			
		RegistryCheck instance = (RegistryCheck) child.children.get(0);
		
		BeanRegistry crRecovered = 
			instance.session.getBeanRegistry();
				
		Object bean = crRecovered.lookup("test");
		assertNotNull(bean);
		assertEquals(ForEachJob.LocalBean.class, bean.getClass());
		
		ForEachJob.LocalBean lb = (ForEachJob.LocalBean) bean;

		int index = lb.getIndex();
		assertEquals(0, index);
		
		String current = (String) lb.getCurrent();
		assertEquals("one", current);
	}
		
    public void testBasic() throws ParseException {
    	
    	checks = new Object[] { 
        		new String("hello"),
        		DateHelper.parseDate("2005-12-25"),
        		null,
        		new File("file.txt")
    		};
    	executed = 0;
    	
        Oddjob oj = new Oddjob();
        oj.setConfiguration(
        		new XMLConfiguration("org/oddjob/jobs/structural/foreach-test.xml",
        				getClass().getClassLoader()));
        oj.run();
        
        // check doesn't get registered!
        Check check = (Check) new OddjobLookup(oj).lookup("check");
        assertNull(check);
        
        assertEquals(4, executed);
        
        oj.destroy();
    }
    
    static Object[] checks;
    static int executed;
    
    public static class Check extends SimpleJob {

        Object o;
        int i;
        
        @ArooaAttribute
        public void setObject(Object o) {
            this.o = o;
        }
        public void setIndex(int i) {
            this.i = i;
        }
        protected int execute() {
            executed++;
            logger.debug("Executing with object [" + o + "]");
            assertEquals(checks[i], o);
            return 0;
        }
    }
      
    public void testReset() throws Exception {
 
    	ChildCatcher childs = new ChildCatcher();
    	
    	String xml =
    			"<foreach xmlns:arooa='http://rgordon.co.uk/oddjob/arooa'>" +
    			"  <values>" +
    			"   <list>" +
    			"    <values>" +
    			"     <value value='COMPLETE'/>" +
    			"     <value value='INCOMPLETE'/>" +
    			"     <value value='COMPLETE'/>" +
    			"    </values>" +
    			"   </list>" +
    			"  </values>" +
    			"  <configuration>" +
    			"     <xml>" +
    			"      <foreach id='e'>" +
    			"       <job>" +
    			"        <bean class='" + FlagState.class.getName() + "' state='${e.current}'/>" +
    			"       </job>" +
    			"      </foreach>" +
    			"     </xml>" +
				"  </configuration>" +
    			"</foreach>";
    	
    	ForEachJob test = (ForEachJob) Helper.createComponentFromXml(xml);
    	test.addStructuralListener(childs);
    	
    	test.run();
    	
    	assertEquals(JobState.COMPLETE, Helper.getJobState(
    			childs.children.get(0)));
    	assertEquals(JobState.INCOMPLETE, Helper.getJobState(
    			childs.children.get(1)));
    	assertEquals(JobState.READY, Helper.getJobState(
    			childs.children.get(2)));
    	assertEquals(ParentState.INCOMPLETE, Helper.getJobState(
    			test));
    	
    	test.softReset();
    	
    	assertEquals(JobState.COMPLETE, Helper.getJobState(
    			childs.children.get(0)));
    	assertEquals(JobState.READY, Helper.getJobState(
    			childs.children.get(1)));
    	assertEquals(JobState.READY, Helper.getJobState(
    			childs.children.get(2)));
    	assertEquals(ParentState.READY, Helper.getJobState(
    			test));
    	
    	test.run();
    	
    	Stateful child1 = (Stateful) childs.children.get(0); 
    	Stateful child2 = (Stateful) childs.children.get(1); 
    	
    	assertEquals(JobState.COMPLETE, child1.lastStateEvent().getState());
    	assertEquals(JobState.INCOMPLETE, child2.lastStateEvent().getState());
    	assertEquals(ParentState.INCOMPLETE, test.lastStateEvent().getState());
    	
    	assertEquals(3, test.getIndex());
    	
    	test.hardReset();
    	
    	assertEquals(0, test.getIndex());
    	
    	assertEquals(JobState.DESTROYED, child1.lastStateEvent().getState());
    	assertEquals(JobState.DESTROYED, child2.lastStateEvent().getState());
    	
    	assertEquals(0, childs.children.size());
    	
    	test.run();
    	
    	child1 = (Stateful) childs.children.get(0); 
    	child2 = (Stateful) childs.children.get(1); 
    	
    	assertEquals(JobState.COMPLETE, child1.lastStateEvent().getState());
    	assertEquals(JobState.INCOMPLETE, child2.lastStateEvent().getState());
    	assertEquals(ParentState.INCOMPLETE, test.lastStateEvent().getState());
    	
    	test.destroy();
    	
    	assertEquals(0, childs.children.size());
    	
    	assertEquals(JobState.DESTROYED, child1.lastStateEvent().getState());
    	assertEquals(JobState.DESTROYED, child2.lastStateEvent().getState());
    }

    public void testIdenticalIdInForEachConfig() throws Exception {
    	
    	String config = 
    		"<foreach id='e'>" +
    		" <job>" +
    		"  <echo text='${e.current}'/>" +
    		" </job>" +
    		"</foreach>";
    	 
    	String xml =
    			"<oddjob>" +
    			" <job>" +
    			"  <foreach id='e'>" +
    			"   <values>" +
    			"    <list>" +
    			"     <values>" +
    			"      <value value='apple'/>" +
    			"      <value value='orange'/>" +
    			"     </values>" +
    			"    </list>" +
    			"   </values>" +
    			"   <configuration>" +
    			"    <value value='${config}'/>" + 
				"   </configuration>" +
    			"  </foreach>" +
    			" </job>" +
    			"</oddjob>";

    	Oddjob oddjob = new Oddjob();
    	oddjob.setConfiguration(new XMLConfiguration("XML", xml));
    	
		XMLConfigurationType configType = new XMLConfigurationType();
		configType.setXml(config);
    	oddjob.setExport("config", configType);

    	oddjob.run();
    	
    	assertEquals(ParentState.COMPLETE, 
    			oddjob.lastStateEvent().getState());    	
    }

    public void testSerializeForEach() throws IOException, ClassNotFoundException {
    	
    	String xml = 
    			"<foreach>" +
    			" <job>" +
    			"  <echo/>" +
    			" </job>" +
    			"</foreach>";
    	
    	ForEachJob test = new ForEachJob();
    	test.setArooaSession(new OddjobSessionFactory().createSession());
    	test.setConfiguration(new XMLConfiguration("XML", xml));
    	test.setValues(Arrays.asList("a", "b"));
    	test.run();
    	
    	assertEquals(ParentState.COMPLETE, test.lastStateEvent().getState());
    	
    	ForEachJob copy = Helper.copy(test);
    	
    	assertEquals(ParentState.COMPLETE, copy.lastStateEvent().getState());
    	
    }

    private class OurPersister extends MockPersisterBase {

		@Override
		protected void persist(Path path, String id, Object component) {
			assertEquals(new Path("foreach-job-test"), path);
			assertEquals("fe", id);
			try {
				Helper.copy(component);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected Object restore(Path path, String id, ClassLoader classLoader) {
			assertEquals(new Path("foreach-job-test"), path);
			assertEquals("fe", id);
			return null;
		}

    }
    
    public void testForEachPersistenceButNoChildren() throws Exception {
    	
    	String config = 
    		"<foreach>" +
    		" <job>" +
    		"  <echo id='x' text='${fe.current}'/>" +
    		" </job>" +
    		"</foreach>";
    	 
    	String xml =
    			"<oddjob>" +
    			" <job>" +
    			"  <foreach id='fe'>" +
    			"   <values>" +
    			"    <list>" +
    			"     <values>" +
    			"      <value value='apple'/>" +
    			"      <value value='orange'/>" +
    			"     </values>" +
    			"    </list>" +
    			"   </values>" +
    			"   <configuration>" +
    			"    <value value='${config}'/>" + 
				"   </configuration>" +
    			"  </foreach>" +
    			" </job>" +
    			"</oddjob>";

    	Oddjob oddjob = new Oddjob();
    	oddjob.setConfiguration(new XMLConfiguration("XML", xml));
    	
    	XMLConfigurationType configType = new XMLConfigurationType();
    	configType.setXml(config);
    	
    	oddjob.setExport("config", configType);
    	
    	OurPersister persister = new OurPersister();
    	persister.setPath("foreach-job-test");
    	oddjob.setPersister(persister);
    	
    	oddjob.run();
    	
    	assertEquals(ParentState.COMPLETE, 
    			oddjob.lastStateEvent().getState());    	
    }
    
    public void testFileCopyExample() {
    	
    	OurDirs dirs = new OurDirs();
    	
    	File fromDir = dirs.relative("test/io/reference");
    	File toDir = dirs.relative("work/foreach");
    	
    	if (toDir.exists()) {
    		toDir.delete();
    	}
    	toDir.mkdirs();
    	
    	Oddjob oddjob = new Oddjob();
    	oddjob.setConfiguration(new XMLConfiguration(
    			"org/oddjob/jobs/structural/ForEachFilesExample.xml",
    			getClass().getClassLoader()));
    	
    	oddjob.setArgs(new String[] {fromDir.toString(), toDir.toString() });
    	
    	oddjob.run();
    	
    	assertEquals(ParentState.COMPLETE, oddjob.lastStateEvent().getState());
    	
    	oddjob.destroy();
    	
    	assertTrue(new File(toDir, "test1.txt").exists());
    	assertTrue(new File(toDir, "test2.txt").exists());
    	assertTrue(new File(toDir, "test3.txt").exists());
    	
    }
    
    public void testWithIds() throws ArooaPropertyException, ArooaConversionException {
    	
    	Oddjob oddjob = new Oddjob();
    	oddjob.setConfiguration(new XMLConfiguration(
    			"org/oddjob/jobs/structural/ForEachWithIdsExample.xml",
    			getClass().getClassLoader()));

		ConsoleCapture console = new ConsoleCapture();
		console.capture(Oddjob.CONSOLE);
				
    	oddjob.run();
    	
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
		console.close();
		console.dump(logger);
		
    	OddjobLookup lookup = new OddjobLookup(oddjob);
    	
    	Structural foreach = lookup.lookup("foreach", Structural.class);
    	
    	ChildCatcher catcher = new ChildCatcher();
    	foreach.addStructuralListener(catcher);
    	
    	assertEquals(3, catcher.children.size());
    	
    	assertEquals("Red", catcher.children.get(0).toString());
    	assertEquals("Blue", catcher.children.get(1).toString());
    	assertEquals("Green", catcher.children.get(2).toString());
    	    	
		String[] lines = console.getLines();
		
		assertEquals(3, lines.length);
		
		assertEquals("I'm number 0 and my name is Red", lines[0].trim());
		assertEquals("I'm number 1 and my name is Blue", lines[1].trim());
		assertEquals("I'm number 2 and my name is Green", lines[2].trim());
    	
    	oddjob.destroy();    	
    }
    
    public void testPropertiesInChildren() {
    	
    	String config = 
    		"<foreach  id='fe'>" +
    		" <job>" +
    		"  <sequential>" +
    		"   <jobs>" +
    		"    <properties>" +
    		"     <values>" +
    		"      <value key='my.fruit' value='${fe.current}'/>" +
    		"     </values>" +
    		"    </properties>" +
    		"    <echo text='${my.fruit}'/>" +
    		"   </jobs>" +
    		"  </sequential>" +
    		" </job>" +
    		"</foreach>";
    	 
    	String xml =
    			"<oddjob>" +
    			" <job>" +
    			"  <foreach>" +
    			"   <values>" +
    			"    <list>" +
    			"     <values>" +
    			"      <value value='apple'/>" +
    			"      <value value='orange'/>" +
    			"     </values>" +
    			"    </list>" +
    			"   </values>" +
    			"   <configuration>" +
    			"    <value value='${config}'/>" + 
				"   </configuration>" +
    			"  </foreach>" +
    			" </job>" +
    			"</oddjob>";

    	Oddjob oddjob = new Oddjob();
    	oddjob.setConfiguration(new XMLConfiguration("XML", xml));
    	
    	XMLConfigurationType configType = new XMLConfigurationType();
    	configType.setXml(config);
    	
    	oddjob.setExport("config", configType);
    	
		ConsoleCapture console = new ConsoleCapture();
		console.capture(Oddjob.CONSOLE);
				
    	oddjob.run();
    	
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
		console.close();
		console.dump(logger);
		
		String[] lines = console.getLines();
		
		assertEquals(2, lines.length);
		
		assertEquals("apple", lines[0].trim());
		assertEquals("orange", lines[1].trim());
    	
    	oddjob.destroy();    	
    }
    
    public void testStop() {
    	
    	String xml = 
    		"<foreach>" +
    		" <job>" +
    		"  <wait/>" +
    		" </job>" +
    		"</foreach>";
    	
    	final ForEachJob test = new ForEachJob();
    	
    	test.setArooaSession(new OddjobSessionFactory().createSession());
    	test.setConfiguration(new XMLConfiguration("XML", xml));
  
    	test.setValues(Arrays.asList("apple", "orange"));
    
    	test.addStructuralListener(new StructuralListener() {
			
			@Override
			public void childRemoved(StructuralEvent event) {
			}
			
			@Override
			public void childAdded(StructuralEvent event) {
				Stateful child = (Stateful) event.getChild();
				child.addStateListener(new StateListener() {
					
					@Override
					public void jobStateChange(StateEvent event) {
						if (event.getState().isStoppable()) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										test.stop();
									} catch (FailedToStopException e) {
										throw new RuntimeException(e);
									}
								}
							}).start();
						}
					}
				});
			}
		});
    	
    	test.run();

    	Object[] children = Helper.getChildren(test);
    	
    	assertEquals(2, children.length);
    	
    	assertEquals(JobState.COMPLETE, Helper.getJobState(children[0]));
    	assertEquals(JobState.READY, Helper.getJobState(children[1]));
    }
    
    /**
     * Tracking down a bug where execution services weren't getting passed in to the
     * internal configuration.
     */
    public void testAutoInject() {
    	
    	String forEachConfig =
    		"<foreach id='test'>" +
    		" <job>" +
    		"  <parallel>" +
    		"   <jobs>" +
    		"    <echo text='Hello'/>" +
    		"   </jobs>" +
    		"  </parallel>" +
    		" </job>" +
    		"</foreach>";
    	
    	String ojConfig = 
    		"<oddjob xmlns:arooa='http://rgordon.co.uk/oddjob/arooa'>" +
    		" <job>" +
    		"  <foreach>" +
    		"   <values>" +
    		"    <list>" +
    		"     <values>" +
    		"      <value value='1'/>" +
    		"      <value value='2'/>" +
    		"     </values>" +
    		"    </list>" +
    		"   </values>" +
    		"   <configuration>" +
    		"    <arooa:configuration>" +
    		"     <xml>" +
    		"      <xml>" + forEachConfig + "</xml>" +
    		"     </xml>" +
    		"    </arooa:configuration>" +
    		"   </configuration>" +
    		"  </foreach>" +
    		" </job>" +
    		"</oddjob>";
    	
    	Oddjob oddjob = new Oddjob();
    	
    	oddjob.setConfiguration(new XMLConfiguration("XML" , ojConfig));

    	ConsoleCapture console = new ConsoleCapture();
    	console.capture(Oddjob.CONSOLE);
    	
    	oddjob.run();

    	String[] lines = console.getLines();
    	assertEquals(2, lines.length);
    	
    	oddjob.destroy();
    }    
}
