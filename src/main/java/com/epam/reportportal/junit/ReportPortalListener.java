/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.epam.reportportal.listeners.Statuses;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 * 
 * @author Aliaksei_Makayed (modified by Andrei_Ramanchuk)
 */

public class ReportPortalListener extends RunListener {

	private IListenerHandler handler = JUnitIjectorProvider.getInstance().getBean(IListenerHandler.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testRunStarted(Description description) throws Exception {
		handler.startLaunch();
		handler.initSuiteProcessor(description);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(Description description) throws Exception {
		handler.startSuiteIfRequired(description);
		handler.starTestIfRequired(description);
		if (null == description.getAnnotation(Test.class) && description.getMethodName().contains("#")) {
			handler.startTestMethod(description);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFinished(Description description) throws Exception {
		if (description.getMethodName().contains("#")) {
			handler.stopTestMethod(description);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFailure(Failure failure) throws Exception {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testRunFinished(Result result) throws Exception {
		handler.stopLaunch();
	}
}
