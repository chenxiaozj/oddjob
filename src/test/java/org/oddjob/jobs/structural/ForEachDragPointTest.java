package org.oddjob.jobs.structural;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.oddjob.OddjobSessionFactory;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.ArooaSession;
import org.oddjob.arooa.ConfigurationHandle;
import org.oddjob.arooa.parsing.ArooaContext;
import org.oddjob.arooa.parsing.ConfigurationSession;
import org.oddjob.arooa.parsing.CutAndPasteSupport;
import org.oddjob.arooa.parsing.DragPoint;
import org.oddjob.arooa.xml.XMLArooaParser;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.state.ParentState;
import org.oddjob.tools.OddjobTestHelper;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class ForEachDragPointTest {

	
    @Test
	public void testRootDragPoint() throws SAXException, IOException, ArooaParseException {
		
		ArooaSession session = new OddjobSessionFactory().createSession();
		
		ForEachJob test = new ForEachJob();
		test.setArooaSession(session);
		test.setConfiguration(new XMLConfiguration("XML", "<foreach/>"));
		
		test.load();
		
		ConfigurationSession configurationSession =
				test.provideConfigurationSession();
		
		DragPoint dragPoint = configurationSession.dragPointFor(test);
		
		assertNotNull(dragPoint);
		
		assertFalse(dragPoint.supportsCut());
		assertFalse(dragPoint.supportsPaste());
		
		assertFalse(configurationSession.isModified());
		
		ConfigurationHandle handle = new XMLArooaParser().parse(
				dragPoint);

		ArooaContext xmlDoc = handle.getDocumentContext();
		
		String replacement = "<foreach>" +
				" <job>" +
				"  <echo name='Altered ${loop.current}'>${loop.current}</echo>" +
				" </job>" +
				"</foreach>";
		
		CutAndPasteSupport.replace(xmlDoc.getParent(), xmlDoc, 
				new XMLConfiguration("XML", replacement));
		
		assertFalse(configurationSession.isModified());
		
		handle.save();
		
		assertTrue(configurationSession.isModified());

		// Note that copying the drag point still has the old version.
		String copy = dragPoint.copy();
		
		Diff diff = DiffBuilder.compare("<foreach/>")
				.withTest(copy).ignoreWhitespace().build();
		
		assertFalse(diff.toString(), diff.hasDifferences());
		
		// Get drag point again.
		dragPoint = configurationSession.dragPointFor(test);
		
		copy = dragPoint.copy();
		
		String expected = "<foreach>" +
				" <job>" +
				"  <echo name='Altered ${loop.current}'><![CDATA[${loop.current}]]></echo>" +
				" </job>" +
				"</foreach>";
		
		diff = DiffBuilder.compare(expected)
				.withTest(copy).ignoreWhitespace().build();

		assertFalse(diff.toString(), diff.hasDifferences());
		
		test.destroy();
	}
	
   @Test
	public void testChildDragPoint() throws ArooaParseException {
		
		ArooaSession session = new OddjobSessionFactory().createSession();
		
		String xml = 
				"<foreach id='loop'>" +
				" <job>" +
				"  <echo name='Echo ${loop.current}'>${loop.current}</echo>" +
				" </job>" +
				"</foreach>";
		
		ForEachJob test = new ForEachJob();
		test.setArooaSession(session);
		test.setValues(Arrays.asList("Apples", "Oranges"));
		test.setConfiguration(new XMLConfiguration("XML", xml));
		
		test.run();
		
		assertEquals(ParentState.COMPLETE, test.lastStateEvent().getState());
		
		Object[] children = OddjobTestHelper.getChildren(test);
		
		assertEquals("Echo Apples", children[0].toString());
		assertEquals("Echo Oranges", children[1].toString());
		
		ConfigurationSession configurationSession =
				test.provideConfigurationSession();
		
		DragPoint dragPoint = configurationSession.dragPointFor(children[0]);
		
		assertNotNull(dragPoint);
		
		assertTrue(dragPoint.supportsCut());
		assertFalse(dragPoint.supportsPaste());
		
		assertFalse(configurationSession.isModified());
		
		ConfigurationHandle handle = new XMLArooaParser().parse(
				dragPoint);
		
		ArooaContext xmlDoc = handle.getDocumentContext();
		
		CutAndPasteSupport.replace(xmlDoc.getParent(), xmlDoc, 
				new XMLConfiguration("XML", 
						"<echo name='Altered ${loop.current}'>${loop.current}</echo>"));
		handle.save();
		
		assertTrue(configurationSession.isModified());
		
		children = OddjobTestHelper.getChildren(test);
		
		assertEquals("Altered Apples", children[0].toString());
		assertEquals("Echo Oranges", children[1].toString());
		
		test.destroy();
	}
	
}
