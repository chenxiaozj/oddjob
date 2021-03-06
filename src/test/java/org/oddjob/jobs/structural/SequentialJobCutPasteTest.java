package org.oddjob.jobs.structural;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import org.oddjob.OjTestCase;

import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.ConfigurationHandle;
import org.oddjob.arooa.deploy.annotations.ArooaAttribute;
import org.oddjob.arooa.parsing.ArooaContext;
import org.oddjob.arooa.parsing.CutAndPasteSupport;
import org.oddjob.arooa.parsing.CutAndPasteSupport.ReplaceResult;
import org.oddjob.arooa.parsing.DragPoint;
import org.oddjob.arooa.parsing.DragTransaction;
import org.oddjob.arooa.registry.ChangeHow;
import org.oddjob.arooa.types.ArooaObject;
import org.oddjob.arooa.xml.XMLArooaParser;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.structural.StructuralEvent;
import org.oddjob.structural.StructuralListener;

/**
 * Cut and Paste Tests.
 */
public class SequentialJobCutPasteTest extends OjTestCase {

	public static class ResultsJob implements Runnable {
		
		public String value;
		
		public List<String> results;
		
		public void run() {
			results.add(value);
		}
		
		@ArooaAttribute
		public void setResults(List<String> results) {
			this.results = results;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return "Results: " + value;
		}
	}
	
	class ChildCatcher implements StructuralListener {
		final List<Object> children = new ArrayList<Object>();
		
		public void childAdded(StructuralEvent event) {
			children.add(event.getIndex(), event.getChild());
		}
		public void childRemoved(StructuralEvent event) {
			children.remove(event.getIndex());
		}
	}
	
	String xml =
		"<oddjob id='this'>" +
		" <job>" +
		"  <sequential id='seq'/>" +
		" </job>" +
		"</oddjob>";

	String red = 
		"<bean class='" + ResultsJob.class.getName() + "'" +
				" results='${results}' value='red'/>";
	
	String amber = 
		"<bean class='" + ResultsJob.class.getName() + "'" +
				" results='${results}' value='amber'" +
				" id='amber'/>";
	
	String green = 
		"<bean class='" + ResultsJob.class.getName() + "'" +
				" results='${results}' value='green'/>";
	
   @Test
	public void testCutAndPaste() throws ArooaParseException {

		List<String> results = new ArrayList<String>();
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		oddjob.setExport("results", new ArooaObject(results));
		oddjob.run();

		SequentialJob test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq"); 

		ChildCatcher childCatcher = new ChildCatcher();
		test.addStructuralListener(childCatcher);

		assertEquals(0, childCatcher.children.size());
		
		DragPoint point = oddjob.provideConfigurationSession().dragPointFor(
				test);
		
		DragTransaction trn = point.beginChange(ChangeHow.FRESH);
		point.paste(-1, green);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(0, red);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(1, amber);
		trn.commit();

		test.hardReset();
		test.run();
		
		assertEquals(3, results.size());
		assertEquals("red", results.get(0));
		assertEquals("amber", results.get(1));
		assertEquals("green", results.get(2));
		
		results.clear();
		
		// simulate cut and paste...
		DragPoint amberPoint = oddjob.provideConfigurationSession().dragPointFor(
				new OddjobLookup(oddjob).lookup("amber"));

		DragTransaction transaction = amberPoint.beginChange(ChangeHow.FRESH);
		
		point.paste(0, amberPoint.copy());
		
		amberPoint.cut();
		
		transaction.commit();
		
		test.hardReset();
		test.run();
		
		assertEquals(3, results.size());
		assertEquals("amber", results.get(0));
		assertEquals("red", results.get(1));
		assertEquals("green", results.get(2));

		results.clear();
		
		// and now cut it out just to make sure it bedded in proper...
		
		amberPoint = oddjob.provideConfigurationSession().dragPointFor(
				new OddjobLookup(oddjob).lookup("amber"));
		
		trn = amberPoint.beginChange(ChangeHow.FRESH);
		amberPoint.cut();
		trn.commit();
		
		test.hardReset();
		test.run();
		
		assertEquals(2, results.size());
		assertEquals("red", results.get(0));
		assertEquals("green", results.get(1));
		
		assertEquals(2, childCatcher.children.size());
		
		oddjob.destroy();
	}
	
   @Test
	public void testBadSave() throws ArooaParseException {

		List<String> results = new ArrayList<String>();
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		oddjob.setExport("results", new ArooaObject(results));
		oddjob.run();

		SequentialJob test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq"); 
		
		DragPoint point = oddjob.provideConfigurationSession().dragPointFor(
				test);
		
		DragTransaction trn = point.beginChange(ChangeHow.FRESH);
		point.paste(-1, green);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(0, red);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(1, amber);
		trn.commit();
				
		DragPoint amber = oddjob.provideConfigurationSession().dragPointFor(
				new OddjobLookup(oddjob).lookup("amber"));
		

		XMLArooaParser xmlParser = new XMLArooaParser();
		
		ConfigurationHandle handle = xmlParser.parse(
				amber);

		ArooaContext xmlDoc = handle.getDocumentContext();
		
		CutAndPasteSupport.replace(xmlDoc.getParent(), xmlDoc, 
				new XMLConfiguration("Replace", "<rubbish/>"));		
		
		try {
			handle.save();
			fail("Should fail.");
		} catch (Exception e) {
			// expected.
		}
		
		test.hardReset();
		test.run();
		
		assertEquals(3, results.size());
		assertEquals("red", results.get(0));
		assertEquals("amber", results.get(1));
		assertEquals("green", results.get(2));
		
		oddjob.destroy();
	}
	
	/**
	 * Now try replacing the whole sequntial job.
	 * 
	 * @throws ArooaParseException
	 */
   @Test
	public void testBadSave2() throws ArooaParseException {

		List<String> results = new ArrayList<String>();
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		oddjob.setExport("results", new ArooaObject(results));
		oddjob.run();

		SequentialJob test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq"); 
		
		DragPoint point = oddjob.provideConfigurationSession().dragPointFor(
				test);
		
		DragTransaction trn = point.beginChange(ChangeHow.FRESH);
		point.paste(-1, green);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(0, red);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(1, amber);
		trn.commit();
				
		DragPoint sequential = oddjob.provideConfigurationSession().dragPointFor(
				new OddjobLookup(oddjob).lookup("seq"));
		

		XMLArooaParser xmlParser = new XMLArooaParser();
		
		ConfigurationHandle handle = xmlParser.parse(
				sequential);

		ArooaContext xmlDoc = handle.getDocumentContext();
		
		CutAndPasteSupport.ReplaceResult result = 
			CutAndPasteSupport.replace(xmlDoc.getParent(), xmlDoc, 
				new XMLConfiguration("Replace", "<rubbish/>"));		
		if (result.getException() != null) {
			throw result.getException();
		}
		
		try {
			handle.save();
			fail("Should fail.");
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		
		test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq");
		
		test.hardReset();
		test.run();
		
		assertEquals(3, results.size());
		assertEquals("red", results.get(0));
		assertEquals("amber", results.get(1));
		assertEquals("green", results.get(2));
		
		oddjob.destroy();
	}
	
	/**
	 * Now try replacing a whole sequential job with one bad child.
	 * 
	 * @throws ArooaParseException
	 */
   @Test
	public void testBadSave3() throws ArooaParseException {

		List<String> results = new ArrayList<String>();
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		oddjob.setExport("results", new ArooaObject(results));
		oddjob.run();

		SequentialJob test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq"); 
		
		DragPoint point = oddjob.provideConfigurationSession().dragPointFor(
				test);
		
		DragTransaction trn = point.beginChange(ChangeHow.FRESH);
		point.paste(-1, green);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(0, red);
		trn.commit();
		
		trn = point.beginChange(ChangeHow.FRESH);
		point.paste(1, amber);
		trn.commit();
				
		DragPoint sequential = oddjob.provideConfigurationSession().dragPointFor(
				new OddjobLookup(oddjob).lookup("seq"));
		

		XMLArooaParser xmlParser = new XMLArooaParser();
		
		ConfigurationHandle handle = xmlParser.parse(
				sequential);

		ArooaContext xmlDoc = handle.getDocumentContext();
		
		
		String badXml = 
			    "<sequential id='seq'>" +
				" <jobs>" +
				"  <bean class='" + ResultsJob.class.getName() + "'" +
				"         results='${this.args[0]}'" +
				"         value='red'" +
				"         id='red'/>" +
				"  <bean class='" + ResultsJob.class.getName() + "'" +
				"         results='${this.args[0]}' " +
				"         value='amber' id='amber'/>" +
				"  <rubbish/>" +
				" </jobs>" +
				"</sequential>";
 
		
		ReplaceResult result = CutAndPasteSupport.replace(xmlDoc.getParent(), xmlDoc, 
				new XMLConfiguration("Replace", badXml));		
		
		if (result.getException() != null) {
			throw result.getException();
		}
		
		Object amber = new OddjobLookup(oddjob).lookup("amber");
		assertNotNull(amber);
				
		try {
			handle.save();
			fail("Should fail.");
		} catch (Exception e) {
			// expected.
		}
		
		Object red = new OddjobLookup(oddjob).lookup("red");
		assertNotNull(red);
		
		test = (SequentialJob) new OddjobLookup(
				oddjob).lookup("seq");
		test.hardReset();
		test.run();
		
		assertEquals(3, results.size());
		
		assertEquals(3, results.size());
		assertEquals("red", results.get(0));
		assertEquals("amber", results.get(1));
		assertEquals("green", results.get(2));
		
		oddjob.destroy();
	}
}
