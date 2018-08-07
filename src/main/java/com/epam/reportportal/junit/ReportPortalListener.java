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
import com.nordstrom.automation.junit.MethodWatcher2;
import com.nordstrom.automation.junit.ShutdownListener;
import com.nordstrom.automation.junit.TestClassWatcher;
import com.nordstrom.automation.junit.TestObjectWatcher;
import com.nordstrom.common.base.UncheckedThrow;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.TestClass;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 *
 * @author Aliaksei_Makayed (modified by Andrei_Ramanchuk)
 */

public class ReportPortalListener extends RunListener implements ShutdownListener, MethodWatcher2, TestClassWatcher, TestObjectWatcher {

	private static volatile IListenerHandler handler = JUnitInjectorProvider.getInstance().getBean(IListenerHandler.class);

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
	public void testRunStarted(Description description) throws Exception {
		handler.initSuiteProcessor(description);
	}

	public void reportTestStarted(Description description) throws Exception {
		handler.startSuiteIfRequired(description);
		handler.starTestIfRequired(description);
		handler.startTestMethod(description);
	}

	public void reportTestFinished(Description description) throws Exception {
		handler.stopTestMethod(description);
	}

	public void reportTestFailure(Failure failure) throws Exception {
		handler.clearRunningItemId();
		handler.sendReportPortalMsg(failure);
		handler.markCurrentTestMethod(failure.getDescription(), Statuses.FAILED);
		handler.handleTestSkip(failure.getDescription());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testIgnored(Description description) throws Exception {
		handler.addToFinishedMethods(description);
	}

	@Override
	public void beforeInvocation(Object obj, FrameworkMethod method) {
		try {
			reportTestStarted(LifecycleHooks.describeChild(obj, method));
		} catch (Exception e) {
			
		}
	}

	@Override
	public void afterInvocation(Object obj, FrameworkMethod method, Throwable thrown) {
		Description description = LifecycleHooks.describeChild(obj, method);
		try {
			if (thrown != null) {
				reportTestFailure(new Failure(description, thrown));
			}
			reportTestFinished(description);
		} catch (Exception e) {
			
		}
	}

	@Override
	public void testObjectCreated(Object testObj, TestClass testClass) {
		System.out.println("Test object created " + testObj);
	}

	@Override
	public void testClassCreated(TestClass testClass, Object runner) {
		System.out.println("Test class created " + testClass);
		attachRunnerScheduler(runner);
	}

	@Override
	public void beforeInvocation(FrameworkMethod method) {
		try {
			reportTestStarted(createTestDescription(method));
		} catch (Exception e) {

		}
	}

	@Override
	public void afterInvocation(FrameworkMethod method, Throwable thrown) {
    	Description description = createTestDescription(method);
    	try {
        	if (thrown != null) {
        		reportTestFailure(new Failure(description, thrown));
        	}
			reportTestFinished(description);
		} catch (Exception e) {
			
		}
	}
	
	private static Description createTestDescription(FrameworkMethod method) {
		return Description.createTestDescription(method.getDeclaringClass(),
                method.getName(), method.getAnnotations());
	}

	@Override
	public void onShutdown() {
		try {
			handler.stopLaunch();
		} catch (RestEndpointIOException e) {
			UncheckedThrow.throwUnchecked(e);
		}
	}
	
	private static void attachRunnerScheduler(final Object runner) {
		if (runner instanceof ParentRunner) {
			final ParentRunner<?> parentRunner = (ParentRunner<?>) runner;
			RunnerScheduler scheduler = new RunnerScheduler() {
		        public void schedule(Runnable childStatement) {
		        	if (parentRunner instanceof Suite) {
		        		Description description = Description.createSuiteDescription(runner.getClass());
		        		System.out.println("Running child of suite " + parentRunner);
		        	} else {
		        		System.out.println("Running child of runner " + parentRunner);
		        	}
		        	
		            childStatement.run();
		        }

		        public void finished() {
		        	if (parentRunner instanceof Suite) {
		        		Description description = Description.createSuiteDescription(runner.getClass());
		        		System.out.println("Run finished for suite " + parentRunner);
		        	} else {
		        		System.out.println("Run finished for runner " + parentRunner);
		        	}
		        }
			};
			
			parentRunner.setScheduler(scheduler);
		}
	}
}
