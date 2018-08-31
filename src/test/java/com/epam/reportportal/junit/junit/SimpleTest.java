package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SimpleTest {
	
	@Test
	public void passingTest() {
		assertTrue("This test shouldn't fail", true);
	}
	
}
