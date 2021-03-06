package org.oddjob.beanbus.mega;

import java.lang.reflect.Method;
import java.util.Collection;

import org.oddjob.arooa.ArooaAnnotations;
import org.oddjob.arooa.ArooaBeanDescriptor;
import org.oddjob.arooa.ArooaSession;
import org.oddjob.arooa.ComponentTrinity;
import org.oddjob.arooa.deploy.ArooaAnnotationsUtil;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.reflect.PropertyAccessor;
import org.oddjob.arooa.registry.ComponentPool;
import org.oddjob.beanbus.Destination;
import org.oddjob.beanbus.Outbound;

/**
 * A collection of different {@link OutboundStrategy}s that are applied to 
 * a component to see if it can be adapted to an {@link Outbound}. 
 * 
 * @author rob
 *
 */
public class OutboundStrategies implements OutboundStrategy {

	@Override
	public <T> Outbound<T> outboundFor(Object component,
			ArooaSession session) {
		
		Outbound<T> outbound = 
				isOutboundAlreadyStrategy().outboundFor(
						component, session);
		
		if (outbound == null) {			
			outbound = hasDestinationAnnotationStrategy().outboundFor(
					component, session);
		}
		
		return outbound;
	}
	
	/**
	 * Provides a strategy that checks to see if the component is a
	 * {@link Outbound} already.
	 *  
	 * @return
	 */
	public OutboundStrategy isOutboundAlreadyStrategy() {
				
		return new OutboundStrategy() {
			
			@SuppressWarnings("unchecked")
			@Override
			public <T> Outbound<T> outboundFor(Object component,
					ArooaSession session) {
				if (component instanceof Outbound) { 
					return ((Outbound<T>) component);
				}
				else {
					return null;
				}
			}
		};
	}
	
	/**
	 * Provides a strategy base on the {@link Destination} annotation.
	 * 
	 * @return
	 */
	public OutboundStrategy hasDestinationAnnotationStrategy() {
		return new OutboundStrategy() {
			
			@Override
			public <T> Outbound<T> outboundFor(Object component,
					ArooaSession session) {
				
				// The component may be a proxy so we have to find
				// the wrapped component.
				ComponentPool componentPool = session.getComponentPool();
				ComponentTrinity trinity = componentPool.trinityFor(
						component);
				
				final Object realComponent = trinity.getTheComponent();
				
				final PropertyAccessor accessor = 
						session.getTools().getPropertyAccessor();
				
				ArooaBeanDescriptor beanDescriptor = 
						session.getArooaDescriptor().getBeanDescriptor(
								accessor.getClassName(realComponent), 
								accessor);
				
				ArooaAnnotations annotations = 
						beanDescriptor.getAnnotations();
				
				final Method setToMethod = annotations.methodFor(Destination.class.getName());
				
				if (setToMethod != null) {
					return new Outbound<T>() {
						@Override
						public void setTo(Collection<? super T> destination) {
							try {
								setToMethod.invoke(realComponent, destination);
							} catch (RuntimeException e) {
								throw e;
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					};
				}
				
				final String property = new ArooaAnnotationsUtil(
						annotations).findSingleAnnotatedProperty(
								Destination.class.getName());
				
				if (property != null) {
					return new Outbound<T>() {
						@Override
						public void setTo(Collection<? super T> destination) {
							try {
								accessor.setProperty(realComponent, 
										property, destination);
							} catch (ArooaPropertyException e) {
								throw e;
							} catch (RuntimeException e) {
								throw e;
							}
						}
					};
				}
				
				return null;
			}
		};
	}
}
