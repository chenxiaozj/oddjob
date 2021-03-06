package org.oddjob.designer.elements.schedule;
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
import org.oddjob.schedules.schedules.MonthlySchedule;
import org.oddjob.schedules.units.DayOfMonth;
import org.oddjob.schedules.units.DayOfWeek;
import org.oddjob.schedules.units.WeekOfMonth;
import org.oddjob.tools.OddjobTestHelper;

/**
 *
 */
public class MonthlyScheduleDETest extends OjTestCase {
	private static final Logger logger = LoggerFactory.getLogger(MonthlyScheduleDETest.class);
	
   @Before
   public void setUp() {
		logger.debug("========================== " + getName() + "===================" );
	}

	DesignInstance design;
	
   @Test
	public void testCreateByDay() throws ArooaParseException {
		
		String xml =  
				"<schedules:monthly xmlns:schedules='http://rgordon.co.uk/oddjob/schedules'" +
				" fromDay='5' " +
				" toDay='last'/>";
	
    	ArooaDescriptor descriptor = 
    		new OddjobDescriptorFactory().createDescriptor(
    				getClass().getClassLoader());
		
		DesignParser parser = new DesignParser(
				new StandardArooaSession(descriptor));
		parser.setArooaType(ArooaType.VALUE);
		
		parser.parse(new XMLConfiguration("TEST", xml));
		
		design = parser.getDesign();
		
		assertEquals(MonthlyScheduleDesign.class, design.getClass());
		
		MonthlySchedule test = (MonthlySchedule) OddjobTestHelper.createValueFromConfiguration(
				design.getArooaContext().getConfigurationNode());
		
		assertEquals(new DayOfMonth.Number(5), test.getFromDay());
		assertEquals(DayOfMonth.Shorthands.LAST, test.getToDay());
	}
	
   @Test
	public void testCreate() throws ArooaParseException {
		
		String xml =  
				"<schedules:monthly xmlns:schedules='http://rgordon.co.uk/oddjob/schedules'" +
				" fromDayOfWeek='tuesday' " +
				" toDayOfWeek='wednesday'" +
				" inWeek='second'/>";
	
    	ArooaDescriptor descriptor = 
    		new OddjobDescriptorFactory().createDescriptor(
    				getClass().getClassLoader());
		
		DesignParser parser = new DesignParser(
				new StandardArooaSession(descriptor));
		parser.setArooaType(ArooaType.VALUE);
		
		parser.parse(new XMLConfiguration("TEST", xml));
		
		design = parser.getDesign();
		
		assertEquals(MonthlyScheduleDesign.class, design.getClass());
		
		MonthlySchedule test = (MonthlySchedule) OddjobTestHelper.createValueFromConfiguration(
				design.getArooaContext().getConfigurationNode());
		
		assertEquals(DayOfWeek.Days.TUESDAY, test.getFromDayOfWeek());
		assertEquals(DayOfWeek.Days.WEDNESDAY, test.getToDayOfWeek());
		assertEquals(WeekOfMonth.Weeks.SECOND, test.getFromWeek());
		assertEquals(WeekOfMonth.Weeks.SECOND, test.getToWeek());
	}

	public static void main(String args[]) throws ArooaParseException {

		MonthlyScheduleDETest test = new MonthlyScheduleDETest();
		test.testCreate();
		
		ViewMainHelper view = new ViewMainHelper(test.design);
		view.run();
		
	}

}
