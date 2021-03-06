package org.oddjob.io;

import org.junit.Before;
import org.junit.Test;
import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.OjTestCase;
import org.oddjob.OurDirs;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.convert.ArooaConversionException;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.logging.LoggingPrintStream;
import org.oddjob.tools.ConsoleCapture;
import org.oddjob.tools.FragmentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class StdoutTypeTest extends OjTestCase {

	private static final Logger logger = LoggerFactory.getLogger(StdoutTypeTest.class);
	
   @Before
   public void setUp() throws Exception {
		logger.debug("-------------------  " + getName() + "  --------------");
	}
	
	
	String EOL = System.getProperty("line.separator");
	
    @Test
	public void testSimple() throws ArooaConversionException, IOException {
		
		ConsoleCapture results = new ConsoleCapture();
		try (ConsoleCapture.Close close = results.captureConsole()) {

			OutputStream output = System.out;

			// Note that depending on order of test different class loader
			// could be capturing console.
			assertEquals(LoggingPrintStream.class.getName(),
					output.getClass().getName());

			OutputStream test = new StdoutType().toValue();

			test.write(("Hello World." + EOL).getBytes());

			test.close();

		}
		
		results.dump(logger);
		
		assertEquals("Hello World.", results.getAll());
	}
	
   @Test
	public void testStdoutInOddjob() throws ArooaPropertyException, ArooaConversionException {
		
		String xml =
			"<oddjob>" +
			" <job>" +
			"  <sequential>" +
			"   <jobs>" +
			"    <copy>" +
			"     <input>" +
			"      <identify id='hello'>" +
			"       <value>" +
			"        <buffer>Hello" + EOL + "</buffer>" +
			"       </value>" + 
			"      </identify>" + 
			"     </input>" + 
			"     <output>" +
			"      <stdout/>" +
			"     </output>" +
			"    </copy>" +
			"    <copy>" +
			"     <input>" +
			"      <buffer>World" + EOL + "</buffer>" + 
			"     </input>" + 
			"     <output>" +
			"      <stdout/>" +
			"     </output>" +
			"    </copy>" +
			"   </jobs>" +
			"  </sequential>" +
			" </job>" +
			"</oddjob>";
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		
		ConsoleCapture results = new ConsoleCapture();
		try (ConsoleCapture.Close close = results.captureConsole()) {
			
			oddjob.run();
		}
		
		String sanityCheck = new OddjobLookup(oddjob).lookup("hello", String.class);
		assertEquals("Hello", sanityCheck.trim());
		
		oddjob.destroy();
		
		results.dump(logger);
		
		String[] lines = results.getLines();
		
		assertEquals("Hello", lines[0]);
		assertEquals("World", lines[1]);
	}
	
   @Test
	public void testExample() throws ArooaParseException {

		OurDirs dirs = new OurDirs();
		
		Properties properties = new Properties();
		properties.setProperty("my.file", dirs.relative(
				"test/io/TestFile.txt").getPath());
		
		FragmentHelper helper = new FragmentHelper();
		helper.setProperties(properties);
		
		Runnable copy = (Runnable) helper.createComponentFromResource(
				"org/oddjob/io/StdoutTypeExample.xml");
		
		ConsoleCapture results = new ConsoleCapture();
		try (ConsoleCapture.Close close = results.captureConsole()) {
			
			copy.run();
		}
		
		String[] lines = results.getLines();
		
		assertEquals("Test", lines[0].trim());
		assertEquals(1, lines.length);
	}
}
