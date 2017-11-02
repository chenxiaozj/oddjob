package org.oddjob.values.types;

import org.junit.Test;

import java.text.ParseException;

import org.oddjob.OjTestCase;

import org.apache.log4j.Logger;
import org.oddjob.Oddjob;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.state.ParentState;
import org.oddjob.tools.ConsoleCapture;

public class TokenizerTypeTest extends OjTestCase {

	private static final Logger logger = Logger.getLogger(
			TokenizerTypeTest.class);
	
   @Test
	public void testSimpleCSV() throws ParseException {
		
		TokenizerType test = new TokenizerType();
		test.setText("a, b, c");
		
		String[] result = test.parse();
		
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
		assertEquals("c", result[2]);
	}
	
   @Test
	public void testTabDelimited() throws ParseException {
		
		TokenizerType test = new TokenizerType();
		test.setText("a\tb\tc");
		test.setDelimiter("\t");
		
		String[] result = test.parse();
		
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
		assertEquals("c", result[2]);
	}
	
   @Test
	public void testRegexpDelimited() throws ParseException {
		
		TokenizerType test = new TokenizerType();
		test.setText("a,b;c");
		test.setDelimiter("(,|;)");
		test.setRegexp(true);
		
		String[] result = test.parse();
		
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
		assertEquals("c", result[2]);
	}
	
   @Test
	public void testQuoted() throws ParseException {
		
		TokenizerType test = new TokenizerType();
		test.setText("a,'b,c'");
		test.setQuote('\'');
		
		String[] result = test.parse();
		
		assertEquals("a", result[0]);
		assertEquals("b,c", result[1]);
	}
	
   @Test
	public void testExample() {
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration(
				"org/oddjob/values/types/TokenizerExample.xml",
				getClass().getClassLoader()));

		ConsoleCapture console = new ConsoleCapture();
		try (ConsoleCapture.Close close = console.captureConsole()) {
			
			oddjob.run();
		}
		
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
		console.dump(logger);
		
		String[] lines = console.getLines();
		
		assertEquals("I'm 1", lines[0].trim());
		assertEquals("I'm 2", lines[1].trim());
		assertEquals("I'm 3", lines[2].trim());
		assertEquals("I'm 4", lines[3].trim());
		
		assertEquals(12, lines.length);
		
		oddjob.destroy();
	}
}
