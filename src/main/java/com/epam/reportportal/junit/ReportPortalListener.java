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
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.MethodWatcher;
import com.nordstrom.automation.junit.RunWatcher;
import com.nordstrom.automation.junit.RunnerWatcher;
import com.nordstrom.automation.junit.ShutdownListener;

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
public class ReportPortalListener implements ShutdownListener, RunnerWatcher, RunWatcher, MethodWatcher {

	private static volatile IListenerHandler handler;

	static {
		handler = JUnitInjectorProvider.getInstance().getInstance(IListenerHandler.class);
		handler.startLaunch();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onShutdown() {
		handler.stopLaunch();
	}

	@Override
	public void runStarted(Object runner) {
		boolean isSuite = (runner instanceof Suite);
		handler.startRunner(runner, isSuite);
	}

	@Override
	public void runFinished(Object runner) {
		handler.stopRunner(runner);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(FrameworkMethod method, TestClass testClass) {
		// we're not tracking "atomic" tests, so nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFinished(FrameworkMethod method, TestClass testClass) {
		// we're not tracking "atomic" tests, so nothing to do here
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
		handler.handleTestSkip(method, testClass);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeInvocation(Object target, FrameworkMethod method, Object... params) {
		TestClass testClass = LifecycleHooks.getTestClassWith(method);
		handler.startTestMethod(method, testClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterInvocation(Object target, FrameworkMethod method, Throwable thrown) {
		TestClass testClass = LifecycleHooks.getTestClassWith(method);
		if (thrown != null) {
			reportTestFailure(method, testClass, thrown);
		}
		
		handler.stopTestMethod(method);
	}

	/**
	 * Report failure of the indicated "particle" method.
	 * 
	 * @param method {@link FrameworkMethod} object for the "particle" method
	 * @param testClass {@link TestClass} object for the associated "atomic" test
	 * @throws RestEndpointIOException is something goes wrong
	 */
	public void reportTestFailure(FrameworkMethod method, TestClass testClass, Throwable thrown) {
		handler.sendReportPortalMsg(method, thrown);
		handler.markCurrentTestMethod(method, Statuses.FAILED);
	}

}
