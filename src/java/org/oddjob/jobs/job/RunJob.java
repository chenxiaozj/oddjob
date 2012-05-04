package org.oddjob.jobs.job;

import java.util.LinkedList;

import org.oddjob.OddjobComponentResolver;
import org.oddjob.Stateful;
import org.oddjob.Stoppable;
import org.oddjob.Structural;
import org.oddjob.arooa.deploy.annotations.ArooaAttribute;
import org.oddjob.arooa.design.DesignFactory;
import org.oddjob.arooa.parsing.ArooaElement;
import org.oddjob.arooa.parsing.ConfigurationOwner;
import org.oddjob.arooa.parsing.ConfigurationSession;
import org.oddjob.arooa.parsing.OwnerStateListener;
import org.oddjob.framework.ComponentBoundry;
import org.oddjob.framework.StructuralJob;
import org.oddjob.images.IconHelper;
import org.oddjob.state.IsAnyState;
import org.oddjob.state.IsHardResetable;
import org.oddjob.state.IsSoftResetable;
import org.oddjob.state.IsStoppable;
import org.oddjob.state.ParentState;
import org.oddjob.state.State;
import org.oddjob.state.StateEvent;
import org.oddjob.state.StateListener;
import org.oddjob.state.StateOperator;
import org.oddjob.state.WorstStateOp;
import org.oddjob.util.OddjobConfigException;

/**
 * @oddjob.description A job which runs another job. The other job can be
 * local or or on a server.
 * <p>
 * This job reflects the state of the job being executed.
 * <p>
 * 
 * @oddjob.example
 * 
 * Examples elsewhere.
 * <ul>
 *  <li>The {@link org.oddjob.jmx.JMXClientJob} job has an
 *  example that uses <code>run</code> to run a job on a 
 *  remote server.</li>
 * </ul>
 *  
 * 
 * @author Rob Gordon
 */

public class RunJob extends StructuralJob<Object>
implements Structural, Stoppable, ConfigurationOwner {
    private static final long serialVersionUID = 20050806201204300L;

	/** 
	 * @oddjob.property
	 * @oddjob.description Job to run
	 * @oddjob.required Yes.
	 */
	private transient Object job;
	
	private transient ParentState lastState = null;
	
	/**
	 * Set the stop node directly.
	 * 
	 * @param node The job.
	 */
	@ArooaAttribute
	synchronized public void setJob(Object node) {
		this.job = node;
	}

	/**
	 * Get the job.
	 * 
	 * @return The node.
	 */
	synchronized public Object getJob() {
		return this.job;
	}	
	
	@Override
	protected StateOperator getStateOp() {
		return new StateOperator() {
			
			@Override
			public ParentState evaluate(State... states) {
				if (states.length > 0 && states[0].isDestroyed()) {
					ComponentBoundry.push(loggerName(), RunJob.this);
					try {
						logger().info("Job Destroyed, setting to previous state " + 
									lastState);
						try {
							return lastState;
						}
						finally {
							childStateReflector.stop();
							childHelper.removeAllChildren();
						}
					}
					finally {
						ComponentBoundry.pop();
					}
				}
				lastState = new WorstStateOp().evaluate(states);
				return lastState;
			}
		};
	}

	
	/*
	 *  (non-Javadoc)
	 * @see org.oddjob.jobs.AbstractJob#execute()
	 */
	protected void execute() throws Exception {
		
		if (job == null) {
			throw new OddjobConfigException("A job to start must be provided.");
		}
		
		Object proxy;
		if (childHelper.size() == 0) {
			OddjobComponentResolver resolver = new OddjobComponentResolver();
			
			proxy = resolver.resolve(job, getArooaSession());
			childHelper.addChild(proxy);
		}
		else {
			proxy = childHelper.getChild();
		}
			
		final LinkedList<State> states = new LinkedList<State>();
		StateListener listener = null;
		
		if (job instanceof Stateful) {
			listener = new StateListener() {
				synchronized public void jobStateChange(StateEvent event) {
					synchronized (states) {
						states.add(event.getState());
						stateHandler().waitToWhen(new IsAnyState(), new Runnable() {
							public void run() {
								stateHandler.wake();
							}
						});
					}
				}
			};
			
			((Stateful) job).addStateListener(listener);
		}
		
		if (proxy instanceof Runnable) {
			Runnable runnable = (Runnable) proxy;
			runnable.run();
		}
		
		if (job instanceof Stateful) {
			
			boolean executed = false;
			try {
				while (!stop) {
	
					State now = null;
					
					synchronized (states) {
						if (!states.isEmpty()) {
							now = states.removeFirst();
							logger().debug("State received "+ now);
						}
					}
					
					if (now != null) {
						if (now.isDestroyed()) {
							childHelper.removeAllChildren();
							throw new IllegalStateException("Job Destroyed.");
						}
						if (now.isStoppable()) {
							executed = true;
						}
						// when the thread of control has moved has passed a job.						
						if (now.isPassable() && 
								(executed || !now.isReady())) {
							logger().debug("Job has executed. State is " + now);
							lastState = getStateOp().evaluate(now);
							break;
						}
						continue;
					}
					
					logger().debug("Waiting for job to finish executing");
					
					sleep(0);
				}
			}
			finally {
				((Stateful) job).removeStateListener(listener);		
			}
		}
	}

	protected void sleep(final long waitTime) {
		stateHandler().assertAlive();
		
		if (!stateHandler().waitToWhen(new IsStoppable(), new Runnable() {
			public void run() {
				if (stop) {
					logger().debug("Stop request detected. Not sleeping.");
					
					return;
				}
				
				logger().debug("Sleeping for " + ( 
						waitTime == 0 ? "ever" : "[" + waitTime + "] milli seconds") + ".");
				
				iconHelper.changeIcon(IconHelper.SLEEPING);
					
				try {
					stateHandler().sleep(waitTime);
				} catch (InterruptedException e) {
					logger().debug("Sleep interupted.");
					Thread.currentThread().interrupt();
				}
				
				// Stop should already have set Icon to Stopping.
				if (!stop) {
					iconHelper.changeIcon(IconHelper.EXECUTING);
				}
			}
		})) {
			throw new IllegalStateException("Can't sleep unless EXECUTING.");
		}
	}
	

	/**
	 * Perform a soft reset on the job.
	 */
	public boolean softReset() {
		ComponentBoundry.push(loggerName(), this);
		try {
			return stateHandler.waitToWhen(new IsSoftResetable(), new Runnable() {
				public void run() {
				
					logger().debug("Propergating Soft Reset to children.");			
					
					childStateReflector.stop();
					childHelper.removeAllChildren();
					stop = false;
					getStateChanger().setState(ParentState.READY);
					
					logger().info("Soft Reset complete.");
				}
			});	
		} finally {
			ComponentBoundry.pop();
		}
	}
	
	/**
	 * Perform a hard reset on the job.
	 */
	public boolean hardReset() {
		
		ComponentBoundry.push(loggerName(), this);
		try {
			return stateHandler.waitToWhen(new IsHardResetable(), new Runnable() {
				public void run() {
					logger().debug("Propergating Hard Reset to children.");			
					
					childStateReflector.stop();
					childHelper.removeAllChildren();
					stop = false;
					getStateChanger().setState(ParentState.READY);
					
					logger().info("Hard Reset complete.");
				}
			});
		} finally {
			ComponentBoundry.pop();
		}
	}
	
	
	@Override
	public void addOwnerStateListener(OwnerStateListener listener) {
	}
	@Override
	public void removeOwnerStateListener(OwnerStateListener listener) {
	}
	@Override
	public ConfigurationSession provideConfigurationSession() {
		return null;
	}
	@Override
	public DesignFactory rootDesignFactory() {
		return null;
	}
	@Override
	public ArooaElement rootElement() {
		return null;
	}
}
