package org.oddjob.framework;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watches the execution of jobs and executes an action when all jobs
 * have been executed.
 * 
 * @author rob
 *
 */
public class ExecutionWatcher {

	/** The action to run. */
	private final Runnable action;

	/** The number to count to. */
	private final AtomicInteger added = new AtomicInteger(); 
	
	/** The number executed. */
	private final AtomicInteger executed = new AtomicInteger();
	
	/** Started. */
	private boolean started;
	
	/**
	 * Constructor.
	 * 
	 * @param action The action to run.
	 */
	public ExecutionWatcher(Runnable action) {
		this.action = action;
	}

	/**
	 * Add a job.
	 * 
	 * @param job
	 * @return The new job to execute.
	 */
	public Runnable addJob(final Runnable job) {
		
		added.incrementAndGet();
		
		return new Runnable() {
			
			@Override
			public void run() {
				job.run();
				executed.incrementAndGet();

				boolean perform;
				synchronized (ExecutionWatcher.this) {
					perform = check();
				}
				
				if (perform) {
					action.run();
				}
			}
		};
		
	}

	/**
	 * Starts the check.
	 */
	public void start() {

		boolean perform;
		synchronized (this) {
			started = true;
			perform = check();
		}
		if (perform) {
			action.run();
		}
	}
	
	/**
	 * Checks if all jobs have executed.
	 * 
	 * @return
	 */
	private boolean check() {
		if (started && added.get() == executed.get()) {
			return true;
		}
		else {
			return false;
		}
	}	
}
