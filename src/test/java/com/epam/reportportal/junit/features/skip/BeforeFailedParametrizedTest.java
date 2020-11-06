package com.epam.reportportal.junit.features.skip;

import com.epam.reportportal.annotations.ParameterKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.runners.Parameterized.*;

@RunWith(Parameterized.class)
public class BeforeFailedParametrizedTest {

	@Parameters
	public static Object[] params() {
		return new Object[] { "one", "two, three" };
	}

	private final String parameter;

	public BeforeFailedParametrizedTest(@ParameterKey("param") String param) {
		parameter = param;
	}

	@Before
	public void beforeEachFailed() {
		throw new IllegalStateException("Before each");
	}


	@Test
	public void testBeforeEachFailed() {
		System.out.println("Test: testBeforeEachFailed - " + parameter);
	}
}
