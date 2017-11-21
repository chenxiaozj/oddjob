package org.oddjob.jobs;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.oddjob.OjTestCase;
import org.apache.log4j.Logger;
import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.convert.ArooaConversionException;
import org.oddjob.arooa.logging.Appender;
import org.oddjob.arooa.logging.AppenderAdapter;
import org.oddjob.arooa.logging.LogLevel;
import org.oddjob.arooa.logging.LoggerAdapter;
import org.oddjob.arooa.logging.LoggingEvent;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.logging.slf4j.LogoutType;
import org.oddjob.state.ParentState;
import org.oddjob.tools.ConsoleCapture;
import org.oddjob.tools.OddjobTestHelper;
import org.oddjob.tools.OurDirs;

public class ExecJobExamplesTest extends OjTestCase {
	private static final Logger logger = Logger.getLogger(ExecJobExamplesTest.class);
	
   @Test
	public void testSimpleExamples() throws ArooaParseException {
		
		// Just check the XML.
		
		OddjobTestHelper.createComponentFromConfiguration(
					new XMLConfiguration("org/oddjob/jobs/ExecSimpleExample.xml",
					getClass().getClassLoader()));
		
		OddjobTestHelper.createComponentFromConfiguration(
				new XMLConfiguration("org/oddjob/jobs/ExecSimpleExample2.xml",
				getClass().getClassLoader()));
		
	}
	
   @Test
	public void testEnvironmentExample() throws ArooaPropertyException, ArooaConversionException {
	
		String envCommand;
		
		String os = System.getProperty("os.name").toLowerCase();
		if (os.matches(".*windows.*")) {
			envCommand = "cmd /c set";
		}
		else {
			envCommand = "sh -c set";
		}

		Properties properties = new Properties();
		properties.put("platform.set.command", envCommand);
		
		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration(
				"org/oddjob/jobs/ExecJobEnvironmentExample.xml",
				getClass().getClassLoader()));
		oddjob.setProperties(properties);
		
		oddjob.load();
		
		ExecJob exec = new OddjobLookup(oddjob).lookup("exec", ExecJob.class);
		
		ConsoleCapture console = new ConsoleCapture();
		try (ConsoleCapture.Close close = console.capture(exec.consoleLog())) {
			
			oddjob.run();
		}
		
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
		console.dump(logger);
		
		String[] lines = console.getLines();
		
		boolean found = false;
		for (String line : lines) {
			if (line.contains("ODDJOB_FILE=myfile.txt")) {
				found = true;
			}
		}
		
		assertTrue(found);
		
		oddjob.destroy();
	}
		
   @Test
	public void testWithStdInExample() throws ArooaPropertyException, ArooaConversionException, IOException {
		
		OurDirs dirs = new OurDirs();
		File runJar = dirs.relative("run-oddjob.jar");
		
		Properties properties = new Properties();
		properties.put("oddjob.run.jar", runJar.getCanonicalPath());
		
		Oddjob oddjob = new Oddjob();
		oddjob.setFile(dirs.relative(
				"test/java/org/oddjob/jobs/ExecWithStdInExample.xml"));
		oddjob.setProperties(properties);
		
		oddjob.load();
		
		ExecJob exec = new OddjobLookup(oddjob).lookup("exec", ExecJob.class);
		
		ConsoleCapture console = new ConsoleCapture();
		try (ConsoleCapture.Close close = console.capture(exec.consoleLog())) {
			
			oddjob.run();
		}
		
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
		console.dump(logger);
		
		String[] lines = console.getLines();
		
		assertEquals("apples", lines[0].trim());
		assertEquals("oranges", lines[1].trim());
		assertEquals("pears", lines[2].trim());
		
		assertEquals(3, lines.length);
		
		oddjob.destroy();
	}
	
   @Test
	public void testWithRedirectToFileExample() throws ArooaPropertyException, ArooaConversionException, IOException {
		
		OurDirs dirs = new OurDirs();
		File runJar = dirs.relative("run-oddjob.jar");
		File workDir = dirs.relative("work");
		File output = new File(workDir, "ExecOutput.log");
		if (output.exists()) {
			output.delete();
		}
		
		Properties properties = new Properties();
		properties.put("oddjob.run.jar", runJar.getCanonicalPath());
		properties.put("work.dir", workDir.getCanonicalPath());
		
		Oddjob oddjob = new Oddjob();
		oddjob.setFile(dirs.relative(
				"test/java/org/oddjob/jobs/ExecWithRedirectToFile.xml"));
		oddjob.setProperties(properties);
		
		oddjob.run();
		
		assertEquals(ParentState.COMPLETE, 
				oddjob.lastStateEvent().getState());
		
			
		oddjob.destroy();
	}
	
	
	private class Results implements Appender {
		
		List<Object> info = new ArrayList<Object>();
		List<Object> warn = new ArrayList<Object>();
		
		@Override
		public void append(LoggingEvent arg0) {
			if (arg0.getLevel() == LogLevel.INFO) {
				info.add(arg0.getMessage());
			}
			if (arg0.getLevel() == (LogLevel.WARN)) {
				warn.add(arg0.getMessage());
			}
		}
	}
	
   @Test
	public void testWithRedirectToLogExample() throws IOException {
		
		OurDirs dirs = new OurDirs();
		File runJar = dirs.relative("run-oddjob.jar");
		
		Properties properties = new Properties();
		properties.put("oddjob.run.jar", runJar.getCanonicalPath());

		Oddjob oddjob = new Oddjob();
		oddjob.setConfiguration(new XMLConfiguration(
				"org/oddjob/jobs/ExecWithRedirectToLog.xml", 
				getClass().getClassLoader()));
		oddjob.setProperties(properties);
		
		Results results = new Results();
		
		AppenderAdapter appenderAdapter = LoggerAdapter.appenderAdapterFor(LogoutType.class);
		
		appenderAdapter.addAppender(results);
		
		oddjob.run();
				
		appenderAdapter.removeAppender(results);
		
		assertEquals(ParentState.INCOMPLETE,
				oddjob.lastStateEvent().getState());
		
		assertTrue(results.info.size() == 0);		
		assertTrue(results.warn.size() > 0);		
		
		oddjob.destroy();		
	}
	
}
