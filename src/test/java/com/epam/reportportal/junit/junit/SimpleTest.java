package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleTest {
	
	private static Logger LOGGER = LoggerFactory.getLogger(SimpleTest.class);
	
	@Test
	public void passingTest() {
		LOGGER.info("This is a message from a passing test");
		assertTrue("This test shouldn't fail", true);
	}
	
}
