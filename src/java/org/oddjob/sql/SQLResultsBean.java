package org.oddjob.sql;

import java.util.ArrayList;
import java.util.List;

import org.oddjob.arooa.deploy.annotations.ArooaHidden;
import org.oddjob.beanbus.AbstractDestination;
import org.oddjob.beanbus.BusAware;
import org.oddjob.beanbus.BusConductor;
import org.oddjob.beanbus.BusCrashException;
import org.oddjob.beanbus.BusEvent;
import org.oddjob.beanbus.BusListenerAdapter;

/**
 * @oddjob.description Captures SQL results in a bean that 
 * has properties to provide those results to other jobs.
 * <p>
 * The properties {@code row}, {@code rows}, {@code rowSets} properties
 * in turn expose the results as beans so the colums can be accessed as
 * properties.
 * <p>
 * If a single query result set consisted of a single row:
 * 
 * <code><pre>
 * NAME        AGE
 * John        47
 * </pre></code>
 * 
 * then:
 * 
 * <code><pre>
 * row.NAME == rows[0].NAME == rowSets[0][0].NAME == 'John'
 * row.AGE == rows[0].AGE == rowSets[0][0].AGE == 47
 * </pre></code>
 * 
 * If a single query result set consisted of more than a single row:
 * 
 * <code><pre>
 * NAME        AGE
 * John        47
 * Jane        72
 * </pre></code>
 * 
 * then the {@code row} property is unavailable and any attempt to access
 * it would result in an exception, and:
 * 
 * <code><pre>
 * rows[1].NAME == rowSets[0][1].NAME == 'Jane'
 * rows[1].AGE == rowSets[0][1].AGE == '72'
 * </pre></code>
 * 
 * If the query results in a multiple query result set:
 * 
 * <code><pre>
 * NAME        AGE
 * John        47
 * Jane        72
 * </pre></code>
 * 
 * <code><pre>
 * FRUIT       COLOUR
 * Apple       Green
 * </pre></code>
 * 
 * then the {@code row} property and the {@code rows} properties are 
 * unavailable and any attempt to access either would result in an exception.
 * The rowSets property can be used as follows:
 * 
 * <code><pre>
 * rowSets[0][1].NAME == 'Jane'
 * rowSets[0][1].AGE == '72'
 * rowSets[1][0].FRUIT == 'Apple'
 * rowSets[1][0].COLOUR == 'Green'
 * </pre></code>
 * 
 * The case of the properties depends on the database used.
 * <p>
 * 
 * Any attempt to access a row or row set that doesn't exist will result
 * in an exception.
 * <p>
 * 
 * @oddjob.example
 * 
 * See {@link SQLJob} for an example.
 * 
 * @author rob
 *
 */
public class SQLResultsBean extends AbstractDestination<Object>
implements BusAware {
	
	/** 
	 * @oddjob.property
	 * @oddjob.description The results of the query. This property allows
	 * indexed access to the rows in the result.
	 * @oddjob.required Read only.
	 */
	private final List<List<?>> rowSets = new ArrayList<List<?>>();
	
	/** 
	 * @oddjob.property
	 * @oddjob.description The update count of any insert/update/delete statement.
	 * @oddjob.required Read only. 
	 */
	private final List<Integer> updateCounts = new ArrayList<Integer>(); 
	
	private int rowCount;
	
	private int updateCount;
	
	
	@ArooaHidden
	@Override
	public void setBeanBus(BusConductor bus) {
		bus.addBusListener(new BusListenerAdapter() {
			
			@Override
			public void busStarting(BusEvent event) throws BusCrashException {				
				rowSets.clear();
				updateCounts.clear();
				rowCount = 0;
				updateCount = 0;
			}
			
			@Override
			public void busTerminated(BusEvent event) {
				event.getSource().removeBusListener(this);
			}			
		});
		
	}
	
	@Override
	public boolean add(Object bean) {
		if (bean instanceof List<?>) {
			List<?> beans = (List<?>) bean;
			rowSets.add(beans);
	
			rowCount += beans.size();
		}
		else if (bean instanceof UpdateCount) {
			
			UpdateCount updateCount = (UpdateCount) bean;
			
			updateCounts.add(new Integer(updateCount.getCount()));
			
			this.updateCount += updateCount.getCount();
		}
		else {
			throw new IllegalArgumentException("Unexpected bean type: " +
					bean.getClass());
		}
		
		return true;
	}
	
	/** 
	 * @oddjob.property rowCount
	 * @oddjob.description The total number of rows returned by all the 
	 * queries.
	 * @oddjob.required Read only.
	 */
	public int getRowCount() {
		return rowCount;
	}
	
	/** 
	 * @oddjob.property rowSetCount
	 * @oddjob.description The number of rows sets, which will be the same
	 * as the number of queries that returned results.
	 * 
	 * @oddjob.required Read only.
	 */
	public int getRowSetCount() {
		return rowSets.size();
	}
	
	/** 
	 * @oddjob.property rowSets
	 * @oddjob.description A two dimensional array of all of the rows
	 * that each individual query returned.
	 * 
	 * @oddjob.required Read only.
	 */
	public Object[][] getRowSets() {
		
		Object[][] allSets = new Object[rowSets.size()][];
		int i = 0;
		for (List<?> rows : rowSets ) {
			if (rows == null) {
				allSets[i++] = null;
			}
			else {
				allSets[i++] = rows.toArray(new Object[rows.size()]);
			}
		}
		return allSets;
	}
			
	/** 
	 * @oddjob.property rows
	 * @oddjob.description An array of the rows when the query set contains only 
	 * one result returning query.
	 * If no results were returned by the queries this property is null. If
	 * there are more than one result sets an exception will occur.
	 * @oddjob.required Read only.
	 */
	public Object[] getRows() {
		if (rowSets.size() > 1) {
			throw new UnsupportedOperationException(
					"Properties [row/rows] are not available when there are multiple (" + 
					rowSets.size() + ") row sets. Use rowSets[0] instead.");
		}
		if (rowSets.size() == 0) {
			return null;
		}
		
		List<?> rows = rowSets.get(0); 
		if (rows == null) {
			return null;
		}

		return rows.toArray(new Object[rows.size()]);		
	}
	
	/** 
	 * @oddjob.property row
	 * @oddjob.description The result of a query when only one result is expected. 
	 * If no results were returned by the queries this property is null. If
	 * there are more than one row an exception will occur.
	 * @oddjob.required Read only.
	 */
	public Object getRow() {
		Object[] rows = getRows();
		if (rows == null) {
			return null;
		}
		if (rows.length > 1) {
			throw new UnsupportedOperationException(
					"Property [row] is not available when there are multiple (" + 
					rows.length + ") rows. Use rows[0] instead.");
		}
		if (rows.length == 0) {
			return null;
		}
		
		return rows[0];
	}
	
	/** 
	 * @oddjob.property updateCounts
	 * @oddjob.description An Array of the update counts, one element per
	 * data modification statement.
	 * @oddjob.required Read only.
	 */
	public Integer[] getUpdateCounts() {
		
		return updateCounts.toArray(new Integer[updateCounts.size()]);
	}
	
	/** 
	 * @oddjob.property updateCount
	 * @oddjob.description The total update count for all queries.
	 * @oddjob.required Read only.
	 */
	public int getUpdateCount() {
		
		return updateCount;
	}
}
