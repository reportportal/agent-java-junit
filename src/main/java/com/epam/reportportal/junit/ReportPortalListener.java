/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-junit/
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.junit;

import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.restclient.endpoint.exception.RestEndpointIOException;
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.MethodWatcher;
import com.nordstrom.automation.junit.RunWatcher;
import com.nordstrom.automation.junit.ShutdownListener;
import com.nordstrom.automation.junit.TestClassWatcher;
import com.nordstrom.common.base.UncheckedThrow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 *
 * @author Aliaksei_Makayed (modified by Andrei_Ramanchuk)
 */
public class ReportPortalListener implements ShutdownListener, TestClassWatcher, RunWatcher, MethodWatcher {

	private static volatile IListenerHandler handler;
	private static final Map<TestClass, Object> TESTCLASS_TO_RUNNER = new ConcurrentHashMap<>();

	static {
		handler = JUnitInjectorProvider.getInstance().getBean(IListenerHandler.class);
		try {
			handler.startLaunch();
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onShutdown() {
		try {
			handler.stopLaunch();
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testClassCreated(TestClass testClass, Object runner) {
		TESTCLASS_TO_RUNNER.put(testClass, runner);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testClassStarted(TestClass testClass) {
		Object runner = TESTCLASS_TO_RUNNER.get(testClass);
		boolean isSuite = (runner instanceof Suite);
		
		try {
			handler.startTestClass(testClass, isSuite);
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testClassFinished(TestClass testClass) {
		try {
			handler.stopTestClass(testClass);
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(FrameworkMethod method, TestClass testClass) {
		if (LifecycleHooks.hasConfiguration(testClass)) {
			try {
				handler.startAtomicTest(method, testClass);
			} catch (RestEndpointIOException e) {
				UncheckedThrow.throwUnchecked(e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFinished(FrameworkMethod method, TestClass testClass) {
		if (LifecycleHooks.hasConfiguration(testClass)) {
			try {
				handler.stopAtomicTest(method, testClass);
			} catch (RestEndpointIOException e) {
				UncheckedThrow.throwUnchecked(e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFailure(FrameworkMethod method, TestClass testClass, Throwable thrown) {
		// This is the failure of the "atomic" method. The failure of the "particle" has already been reported.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testAssumptionFailure(FrameworkMethod method, TestClass testClass, AssumptionViolatedException thrown) {
		// This is the failure of the "atomic" method. The failure of the "particle" has already been reported.
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testIgnored(FrameworkMethod method, TestClass testClass) {
		try {
			handler.handleTestSkip(method, testClass);
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeInvocation(Object target, FrameworkMethod method, Object... params) {
		TestClass testClass = LifecycleHooks.getTestClassFor(target);
		try {
			handler.startTestMethod(method, testClass);
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterInvocation(Object target, FrameworkMethod method, Throwable thrown) {
		TestClass testClass = LifecycleHooks.getTestClassFor(target);
		try {
			if (thrown != null) {
				reportTestFailure(method, testClass, thrown);
			}
			
			handler.stopTestMethod(method);
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}

	/**
	 * Report failure of the indicated "particle" method.
	 * 
	 * @param method {@link FrameworkMethod} object for the "particle" method
	 * @param testClass {@link TestClass} object for the associated "atomic" test
	 * @throws RestEndpointIOException is something goes wrong
	 */
	public void reportTestFailure(FrameworkMethod method, TestClass testClass, Throwable thrown)
			throws RestEndpointIOException {
		
		handler.clearRunningItemId();
		handler.sendReportPortalMsg(method, thrown);
		handler.markCurrentTestMethod(method, Statuses.FAILED);
	}
}
