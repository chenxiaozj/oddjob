package org.oddjob.jmx;

import org.junit.Test;

import java.net.MalformedURLException;

import javax.management.remote.JMXServiceURL;

import org.oddjob.OjTestCase;

public class JMXServiceURLHelperTest extends OjTestCase {

   @Test
	public void testFullURL() throws MalformedURLException {

		JMXServiceURLHelper test = new JMXServiceURLHelper();
		
		String url = 
				"service:jmx:rmi:///jndi/rmi://localhost:13013/jmxrmi";
		
		JMXServiceURL result = test.parse(url);
		
		assertEquals(url, result.toString());
	}
	
   @Test
	public void testClientJMXServiceURL() throws MalformedURLException {
		
		JMXServiceURLHelper test = new JMXServiceURLHelper();
		
		JMXServiceURL result;
		
		result = test.parse("my.server");
		
		assertEquals(
				"service:jmx:rmi:///jndi/rmi://my.server/jmxrmi",
				result.toString());
		
		result = test.parse("my.server:13013");
		
		assertEquals(
				"service:jmx:rmi:///jndi/rmi://my.server:13013/jmxrmi",
				result.toString());
		
		result = test.parse("my.server/my-instance");
		
		assertEquals(
				"service:jmx:rmi:///jndi/rmi://my.server/my-instance",
				result.toString());
		
		result = test.parse("my.server:13013/my-instance");
		
		assertEquals(
				"service:jmx:rmi:///jndi/rmi://my.server:13013/my-instance",
				result.toString());
		
	}
	
}