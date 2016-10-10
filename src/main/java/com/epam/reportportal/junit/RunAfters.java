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

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
//import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Class for {@link org.junit.After} and {@link org.junit.AfterClass} annotated
 * method composition
 * 
 * @author Andrei_Ramanchuk
 * @since 07.30.2013
 */
public class RunAfters extends Statement {
	private final Statement fNext;
	private final Object fTarget;
	private final List<FrameworkMethod> fAfters;
	private final RunNotifier fNotifier;
	private final Class<?> fTestClass;
	private MethodType fMethodType;
	List<Throwable> errors = new ArrayList<Throwable>();

	/**
	 * Constructor
	 */
	public RunAfters(Class<?> testClass, Statement next, List<FrameworkMethod> afters, Object target, RunNotifier notifier, MethodType methodType) {
		fTestClass = testClass;
		fNext = next;
		fAfters = afters;
		fTarget = target;
		fNotifier = notifier;
		fMethodType = methodType;
	}

	@Override
	public void evaluate() throws Throwable {
		try {
			fNext.evaluate();
		} catch (Throwable e) { //NOSONAR
			errors.add(e);
		} finally {
			for (FrameworkMethod each : fAfters) {
				Description description = Description.createTestDescription(fTestClass, fMethodType.getPrefix() + each.getName());
				fNotifier.fireTestStarted(description);
				try {
					each.invokeExplosively(fTarget);
				} catch (Throwable e) { //NOSONAR
					errors.add(e);
					fNotifier.fireTestFailure(new Failure(description, e));
				}
				fNotifier.fireTestFinished(description);
			}
		}
		// Commented for a while (has been implemented in original version of
		// method)
		// MultipleFailureException.assertEmpty(errors);
	}

	public boolean isErrors() throws Throwable {
		return !errors.isEmpty();
	}
}
