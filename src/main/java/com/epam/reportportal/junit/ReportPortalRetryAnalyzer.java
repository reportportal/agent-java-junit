package com.epam.reportportal.junit;

import org.junit.runners.model.FrameworkMethod;

import com.nordstrom.automation.junit.JUnitRetryAnalyzer;

public class ReportPortalRetryAnalyzer implements JUnitRetryAnalyzer {

	@Override
	public boolean retry(FrameworkMethod method, Throwable thrown) {
		// TODO Auto-generated method stub
		return false;
	}

}
