package com.epam.reportportal.junit.features.skip;

import org.junit.Ignore;
import org.junit.Test;

public class IgnoredTest {

	@Test
	@Ignore
	public void singleTest() {
		System.out.println("Test class: " + getClass().getCanonicalName());
	}
}
