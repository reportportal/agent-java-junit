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

import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Class for {@link org.junit.Before} and {@link org.junit.BeforeClass}
 * annotated method composition
 * 
 * @author Andrei_Ramanchuk
 * @since 07.30.2013
 */
public class RunBefores extends Statement {
	private final Statement fNext;
	private final RunNotifier fNotifier;
	private final Object fTarget;
	private final List<FrameworkMethod> fBefores;
	private Class<?> fTestClass;
	private MethodType fMethodType;

	/**
	 * Constructor
	 */
	public RunBefores(Class<?> testClass, Statement next, List<FrameworkMethod> befores, Object target, RunNotifier notifier, MethodType methodType) {
		fTestClass = testClass;
		fNext = next;
		fBefores = befores;
		fTarget = target;
		fNotifier = notifier;
		fMethodType = methodType;
	}

	@Override
	public void evaluate() throws Throwable {
		for (FrameworkMethod each : fBefores) {
			Description description = Description.createTestDescription(fTestClass, fMethodType.getPrefix() + each.getName());
			fNotifier.fireTestStarted(description);
			try {
				each.invokeExplosively(fTarget);
			} catch (Throwable e) { //NOSONAR
				fNotifier.fireTestFailure(new Failure(description, e));
			}
			fNotifier.fireTestFinished(description);
		}
		fNext.evaluate();
	}
}
