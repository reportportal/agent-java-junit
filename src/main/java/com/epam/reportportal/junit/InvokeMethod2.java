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

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class InvokeMethod2 extends Statement {

	private static final String STEP = "#step#";
	private final FrameworkMethod fTestMethod;
	private Object fTarget;
	private final RunNotifier fNotifier;
	private final Class<?> fClass;
	private Description description;

	public InvokeMethod2(Class<?> clazz, FrameworkMethod testMethod, Object target, RunNotifier notifier) {
		fTestMethod = testMethod;
		fTarget = target;
		fNotifier = notifier;
		fClass = clazz;
	}

	public Description getDescription(){
		return description;
	}

	@Override
	public void evaluate() {
		description = Description.createTestDescription(fClass, STEP + fTestMethod.getName());
		fNotifier.fireTestStarted(description);
		try {
			fTestMethod.invokeExplosively(fTarget);
		} catch (Throwable e) { //NOSONAR
			fNotifier.fireTestFailure(new Failure(description, e));
		}
		fNotifier.fireTestFinished(description);
	}
}
