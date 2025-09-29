package com.epam.reportportal.junit.features.callback;

import com.epam.reportportal.annotations.Step;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class FailedDescriptionFeatureTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveByDescriptionFeatureTest.class);

	@Test
	public void testWithDescriptionAndException() {
		throw new RuntimeException("Test error message");
	}

	@Test
	public void testWithDescriptionAndAssertException() {
		Assert.assertEquals(0, 1);
	}

	@Test
	public void testWithDescriptionAndStepError() {
		loginWithException();
		Assert.assertTrue(true);
	}

	@Test
	public void testWithDescriptionAndPassed() {
		login();
		Assert.assertTrue(true);
	}

	@Step
	public void login() {
		LOGGER.info("Login class method");
	}

	@Step
	public void loginWithException() {
		throw new NoSuchElementException("Test error message");
	}
}
