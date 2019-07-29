package com.epam.reportportal.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ com.epam.reportportal.junit.junit.DummyTest.class })
public class SuiteTest {

	static {
		System.out.println("<init> SuiteTest");
	}

}
