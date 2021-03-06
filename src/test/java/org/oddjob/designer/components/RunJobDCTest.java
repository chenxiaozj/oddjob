/*
 * (c) Rob Gordon 2005.
 */
package org.oddjob.designer.components;
import org.junit.Before;

import org.junit.Test;

import org.oddjob.OjTestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.oddjob.OddjobDescriptorFactory;
import org.oddjob.arooa.ArooaDescriptor;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.ArooaType;
import org.oddjob.arooa.design.DesignInstance;
import org.oddjob.arooa.design.DesignParser;
import org.oddjob.arooa.design.view.ViewMainHelper;
import org.oddjob.arooa.standard.StandardArooaSession;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.jobs.job.ResetActions;
import org.oddjob.jobs.job.RunJob;
import org.oddjob.tools.OddjobTestHelper;

/**
 *
 */
public class RunJobDCTest extends OjTestCase {
	private static final Logger logger = LoggerFactory.getLogger(RunJobDCTest.class);
	
   @Before
   public void setUp() {
		logger.debug("========================== " + getName() + "===================" );
	}

	DesignInstance design;
	
   @Test
	public void testRun() throws ArooaParseException {
		
		String xml =  
				"<run id='test' name='Test' job='${test}' reset='HARD' "
				+ "join='true' showJob='true'/>";
	
    	ArooaDescriptor descriptor = 
    		new OddjobDescriptorFactory().createDescriptor(
    				getClass().getClassLoader());
		
		DesignParser parser = new DesignParser(
				new StandardArooaSession(descriptor));
		parser.setArooaType(ArooaType.COMPONENT);
		
		parser.parse(new XMLConfiguration("TEST", xml));
		
		design = parser.getDesign();
		
		assertEquals(RunJobDesign.class, design.getClass());
		
		RunJob test = (RunJob) OddjobTestHelper.createComponentFromConfiguration(
				design.getArooaContext().getConfigurationNode());
		
		assertEquals("Test", test.getName());
		assertEquals(test, test.getJob());
		assertEquals(ResetActions.HARD, test.getReset());
		assertEquals(true, test.isShowJob());
		assertEquals(true, test.isJoin());
	}

	public static void main(String args[]) throws ArooaParseException {

		RunJobDCTest test = new RunJobDCTest();
		test.testRun();
		
		ViewMainHelper view = new ViewMainHelper(test.design);
		view.run();
		
	}

}
