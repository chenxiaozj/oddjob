package org.oddjob.schedules.schedules;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;
import org.oddjob.schedules.Interval;
import org.oddjob.schedules.IntervalHelper;
import org.oddjob.schedules.IntervalTo;
import org.oddjob.schedules.Schedule;
import org.oddjob.schedules.ScheduleContext;
import org.oddjob.schedules.ScheduleResult;



/**
 * @oddjob.description This schedule allows a normal schedule 
 * to be broken by the results of another
 * schedule. This might be a list of bank holidays, or time of day, or any other
 * schedule.
 * <p>
 * This schedule works by moving the schedule forward if the start time of the
 * next interval falls within the next interval defined by the break. In the
 * example below for a time of 12:00 on 24-dec-04 the logic is as follows:
 * <ul>
 *   <li>The schedule is next due at 10:00 on the 25-dec-04.</li>
 *   <li>This schedule is within the break, move the schedule on.</li>
 *   <li>The schedule is next due at 10:00 on the 26-dec-04.</li>
 *   <li>This schedule is within the break, move the schedule on.</li>
 *   <li>The schedule is next due at 10:00 on the 27-dec-04.</li>
 *   <li>This schedule is outside the break, use this result.</li>
 * </ul>
 * 
 * @oddjob.example
 * 
 * A schedule that breaks for Christmas.
 * 
 * {@oddjob.xml.resource org/oddjob/schedules/schedules/BrokenScheduleExample.xml}
 * 
 * @author Rob Gordon
 */

public class BrokenSchedule implements Serializable, Schedule{

    private static final long serialVersionUID = 20050226;
    
    private static final Logger logger = Logger.getLogger(BrokenSchedule.class);
    
    /** 
     * @oddjob.property
     * @oddjob.description The schedule. 
     * @oddjob.required Yes.
     */
	private Schedule schedule;
	
	/**
	 * @oddjob.property
	 * @oddjob.description The breaks. 
	 * @oddjob.required Yes.
	 */
	private Schedule breaks;

	/**
	 * @oddjob.property
	 * @oddjob.description The breaks. 
	 * @oddjob.required Yes.
	 */
	private Schedule alternative;
	
	/**
	 * Set the schedule to break up.
	 * 
	 * @param schedule The schedule to break up.
	 */
	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the schedule to break up.
	 * 
	 * @return The schedule to break up.
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}
		
	/**
	 * Set the breaks which will break up the schedule.
	 * 
	 * @param breaks The breaks schedule.
	 */
	public void setBreaks(Schedule breaks) {		
		this.breaks = breaks;
	}
	
	/**
	 * Get the breaks which will break up the schedule.
	 * 
	 * @return The break Schedule.
	 */
	
	public Schedule getBreaks() {
		return this.breaks;
	}

	public Schedule getAlternative() {
		return alternative;
	}

	public void setAlternative(Schedule alternative) {
		this.alternative = alternative;
	}

	/**
	 * Implement the schedule.
	 */
	public ScheduleResult nextDue(ScheduleContext context) {		
		Date now = context.getDate();

		logger.debug(this + ": in interval is " + now);

		// sanity checks
		if (schedule == null) {			
			return null;
		}

	    if (breaks == null) {
			return schedule.nextDue(context);
		}

		Date use = now; 

		// loop until we get a valid interval
		while (true) {
			if (use == null) {
				return null;
			}

			ScheduleResult next = schedule.nextDue(context.move(use));	
			// if the next schedule is never due return.
			if (next == null) {
				return null;
			}

			// find the first exclusion interval
			Interval exclude = mergeBreaks(context.move(next.getFromDate()));
				
			if (exclude == null) {
			    return next;
			}
			// if this interval is before the break
			if (new IntervalHelper(next).isBefore(exclude)) {
				return next;				
			}
			
			// if we got here the last interval is blocked by an exclude.
			
			// first see if there is an alternative.
			if (alternative != null) {
				return alternative.nextDue(context.spawn(use, exclude));
			}
			
			// otherwise move the interval on.

			use = next.getToDate();
		}
	}

	private Interval mergeBreaks(ScheduleContext context) {
		
		ScheduleContext useContext = context;
		
		Interval merged = null;
		while (true) {
			Interval exclude = breaks.nextDue(useContext);
			if (exclude == null) {
				return merged;
			}
			if (merged == null) {
				merged = exclude;
			}
			else {
				if (exclude.getFromDate().after(merged.getToDate())) {
					return merged;
				}
				
				merged = new IntervalTo(merged.getFromDate(), exclude.getToDate());
			}
			
			useContext = useContext.move(merged.getToDate());
		}
	}
	
	
	
	/**
	 * Provide a simple string description.
	 */	
	public String toString() {
		return "Broken Schedule " + schedule + " with breaks " + breaks;
	}
}
