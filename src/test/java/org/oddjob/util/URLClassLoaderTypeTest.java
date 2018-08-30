package org.oddjob.util;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.OjTestCase;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.io.FilesType;
import org.oddjob.tools.CompileJob;
import org.oddjob.tools.OurDirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLClassLoaderTypeTest extends OjTestCase {

	private static final Logger logger = 
		LoggerFactory.getLogger(URLClassLoaderTypeTest.class);
	
    @Before
    public void setUp() throws Exception {

		
		logger.debug("-----------------  " + getName() + "  -----------------");

		logger.debug("ClassLoaders: System=" + 
				ClassLoader.getSystemClassLoader() + ", Context=" +
				Thread.currentThread().getContextClassLoader());
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			logger.debug("URLS: " + Arrays.toString(urls));
		}
	}
	
   @Test
	public void testLoadMixedJob() throws Exception {
		
		ClassLoader existingContextClassLoader = 
				Thread.currentThread().getContextClassLoader();
		
		OurDirs dirs = new OurDirs();
		
		File check = dirs.relative("test/classloader/AJob.class");
		if (!check.exists()) {
			compileSample(dirs.base());
		}
		
		URLClassLoaderType test = new URLClassLoaderType();
		test.setFiles(new File[] { dirs.relative("test/classloader") } );
		test.setParent(getClass().getClassLoader());
		
		assertEquals("URLClassLoaderType: [" + 
				dirs.relative("test/classloader").toString() + "]",
				test.toString());
		
		test.configured();
		ClassLoader classLoader = test.toValue();
		
		Oddjob oddjob = new Oddjob();
		oddjob.setClassLoader(classLoader);
	
		String xml = 
			"<oddjob>" +
			"<job>" +
			"<bean id='test' class='AJob'/>" +
			"</job>" +
			"</oddjob>";
		
		oddjob.setConfiguration(new XMLConfiguration("XML", xml));
		
		oddjob.run();
		
		OddjobLookup lookup = 
			new OddjobLookup(oddjob);
			
		ClassLoader classLoaderWhenRunning = lookup.lookup(
				"test.classLoader", ClassLoader.class);
		
		ClassLoader jobClassLoader = lookup.lookup(
				"test.class.classLoader", ClassLoader.class);
		
		assertEquals(classLoader, jobClassLoader);
		
		assertEquals(classLoader,
				classLoaderWhenRunning);
		
		assertEquals(existingContextClassLoader,
				Thread.currentThread().getContextClassLoader());
	}
	
	static public void compileSample(File oddjobDir) throws IOException {
		
		File dir = new File(oddjobDir, "test/classloader");
		File classFile = new File(dir, "AJob.class");

		if (classFile.exists()) {
			logger.info("{} exist, not compiling." + classFile);
			return;
		}
		
		FilesType sources = new FilesType();
		
		sources.setFiles(dir.getPath() +
				File.separator + "*.java"); 

		File[] srcFiles = sources.toFiles();
		
		logger.info( "Compiling {}", Arrays.toString(srcFiles));
		
		CompileJob compile = new CompileJob();
		compile.setFiles(srcFiles);
		
		compile.run();
		
		if (compile.getResult() != 0) {
			throw new RuntimeException(
					"Compile failed. See standard output for details.");
		}
	}

   @Test
	public void testInOddjob() throws URISyntaxException, IOException {
	
		OurDirs dirs = new OurDirs();
		
		compileSample(dirs.base());
		
    	URL url = getClass().getClassLoader().getResource("org/oddjob/util/URLClassLoader.xml");
    	
    	File file = new File(url.toURI());
		
		Oddjob oddjob = new Oddjob();
		oddjob.setArgs(new String[] { dirs.base().getAbsolutePath() });
		oddjob.setFile(file);
		
		oddjob.run();
		
		Object aJob = new OddjobLookup(oddjob).lookup("nested/x");
		
		assertEquals("AJob", aJob.getClass().getName());
	}
	
   @Test
	public void testNoParent() {
		
		URLClassLoaderType test = new URLClassLoaderType();
		
		test.setFiles(new File[] { });
		test.setNoInherit(true);
		
		test.configured();
		
		ClassLoader loader = test.toValue();
		
		assertNull(loader.getParent());
		
	}
	
}
