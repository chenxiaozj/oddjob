/*
 * Copyright (c) 2004, Rob Gordon.
 */
package org.oddjob.schedules.schedules;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.oddjob.OddjobDescriptorFactory;
import org.oddjob.arooa.ArooaDescriptor;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.standard.StandardFragmentParser;
import org.oddjob.arooa.utils.DateHelper;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.schedules.IntervalTo;
import org.oddjob.schedules.Schedule;
import org.oddjob.schedules.ScheduleContext;

/**
 *
 * @author Rob Gordon.
 */
public class OccurenceScheduleTest extends TestCase {

    static DateFormat checkFormat = new SimpleDateFormat("dd-MMM-yy HH:mm:ss:SSS");
    static DateFormat inputFormat = new SimpleDateFormat("dd-MMM-yy HH:mm");
    
    public void testBasic() throws ParseException {
        OccurrenceSchedule schedule = new OccurrenceSchedule();
        schedule.setOccurrence("2");

        MockSchedule child = new MockSchedule(); 
        child.setResults(new IntervalTo[] {
                new IntervalTo(
                        inputFormat.parse("10-feb-2004 10:00"),
                        inputFormat.parse("10-feb-2004 14:00")),
                new IntervalTo(
                        inputFormat.parse("11-feb-2004 10:00"), 
                        inputFormat.parse("11-feb-2004 14:00")) 
            });        
        schedule.setRefinement(child);
        Date now1 = inputFormat.parse("10-feb-2004 12:30");
        
        IntervalTo expected = new IntervalTo(
                        inputFormat.parse("11-feb-2004 10:00"), 
                        inputFormat.parse("11-feb-2004 14:00")); 
        
        IntervalTo actual = schedule.nextDue(
        		new ScheduleContext(now1));
        
        assertTrue("[" + actual+ "] wrong.", 
        		expected.equals(actual));
    }
    
    public void testOutsideLimits() throws ParseException {
    	
        OccurrenceSchedule test = new OccurrenceSchedule();
        test.setOccurrence("3");

        MockSchedule child = new MockSchedule(); 
        child.setResults(new IntervalTo[] {
                new IntervalTo(
                        inputFormat.parse("10-feb-2004 10:00"),
                        inputFormat.parse("10-feb-2004 14:00")),
                new IntervalTo(
                        inputFormat.parse("11-feb-2004 10:00"), 
                        inputFormat.parse("11-feb-2004 14:00")) 
            });
        
        test.setRefinement(child);
        
        Date now1 = inputFormat.parse("10-feb-2004 12:30");

        IntervalTo expected = null;

        ScheduleContext context = new ScheduleContext(now1);
        context = context.spawn(new IntervalTo(
                inputFormat.parse("10-feb-2004 09:00"), 
                inputFormat.parse("10-feb-2004 15:00")));

        IntervalTo actual = test.nextDue(context);
        
        assertEquals(expected, actual);
    }    
    
    class MockSchedule implements Schedule {
        IntervalTo[] results;
        int next;
        public void setResults(IntervalTo[] results) {
            this.results = results;
            next = 0;
        }
        public void setLimits(IntervalTo limits) {}
        public IntervalTo nextDue(ScheduleContext context) {
            if (results == null) {
                return null;
            }
            if (next < results.length) {
                return results[next++];
            }
            return null;
        }
    }
    
    public void testThirdWednesday() throws ParseException {
    	
    	DayOfMonthSchedule monthly = new DayOfMonthSchedule();
    	
    	OccurrenceSchedule occurrence = new OccurrenceSchedule();
    	occurrence.setOccurrence(new Integer(3).toString());
    	
    	DayOfWeekSchedule day = new DayOfWeekSchedule();
    	day.setOn(3);
    	
    	monthly.setRefinement(occurrence);
    	
    	occurrence.setRefinement(day);
    	
    	ScheduleContext context = 
    		new ScheduleContext(DateHelper.parseDateTime("2009-02-16"));
    	
    	IntervalTo result = monthly.nextDue(context);
    	
    	IntervalTo expected = new IntervalTo(
    			DateHelper.parseDateTime("2009-02-18"),
    			DateHelper.parseDateTime("2009-02-19"));
    	
    	assertEquals(expected, result); 
    }
    
    
    public void testOccurenceExample() throws ArooaParseException, ParseException {
    	
    	OddjobDescriptorFactory df = new OddjobDescriptorFactory();
    	
    	ArooaDescriptor descriptor = df.createDescriptor(
    			getClass().getClassLoader());
    	
    	StandardFragmentParser parser = new StandardFragmentParser(descriptor);
    	
    	parser.parse(new XMLConfiguration(
    			"org/oddjob/schedules/schedules/OccurenceScheduleExample.xml", 
    			getClass().getClassLoader()));
    	
    	Schedule schedule = (Schedule)	parser.getRoot();
    	
    	IntervalTo next = schedule.nextDue(new ScheduleContext(
    			DateHelper.parseDate("2011-02-15")));
    	
    	// TODO: Isn't this wrong????
    	IntervalTo expected = new IntervalTo(
    			DateHelper.parseDateTime("2011-02-08"), 
    			DateHelper.parseDateTime("2011-02-09"));
    	
    	assertEquals(expected, next);
    }
}
