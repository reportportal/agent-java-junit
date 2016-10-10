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

import java.util.List;

import org.junit.*;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * Re-defined JUnit tests runner with Before-After tracking functionality
 * 
 * @author Andrei_Ramanchuk
 * @since 07.30.2013
 */
public class CustomJUnitRunner extends BlockJUnit4ClassRunner {
	private final TestClass fTestClass;
	private final Class<?> testClass;

	/**
	 * Constructor
	 * 
	 * @param klass
	 *            - class which will be executed as test
	 * @throws Exception
	 */
	public CustomJUnitRunner(Class<?> klass) throws Exception {
		super(klass);
		fTestClass = new TestClass(klass);
		testClass = klass;
	}

	protected Statement withBefores(FrameworkMethod method, Object target, Statement statement, RunNotifier notifier) {
		List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
		return befores.isEmpty() ? statement : new RunBefores(testClass, statement, befores, target, notifier, MethodType.BEFORE_METHOD);
	}

	protected Statement withAfters(FrameworkMethod method, Object target, Statement statement, RunNotifier notifier) {
		List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
		return afters.isEmpty() ? statement : new RunAfters(testClass, statement, afters, target, notifier, MethodType.AFTER_METHOD);
	}

	protected Statement withBeforeClasses(Statement statement, RunNotifier notifier) {
		List<FrameworkMethod> befores = fTestClass.getAnnotatedMethods(BeforeClass.class);
		return befores.isEmpty() ? statement : new RunBefores(testClass, statement, befores, null, notifier, MethodType.BEFORE_CLASS);
	}

	protected Statement withAfterClasses(Statement statement, RunNotifier notifier) {
		List<FrameworkMethod> afters = fTestClass.getAnnotatedMethods(AfterClass.class);
		return afters.isEmpty() ? statement : new RunAfters(testClass, statement, afters, null, notifier, MethodType.AFTER_CLASS);
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		Description description = describeChild(method);
		if (method.getAnnotation(Ignore.class) != null) {
			notifier.fireTestIgnored(description);
		} else {
			runLeaf(methodBlock(method, notifier), description, notifier);
		}
	}

	protected Statement methodInvoker2(FrameworkMethod method, Object test, RunNotifier notifier) {
		return new InvokeMethod2(testClass, method, test, notifier);
	}

	@SuppressWarnings("deprecation")
	protected Statement methodBlock(FrameworkMethod method, RunNotifier notifier) {
		Object test;
		try {
			test = new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return createTest();
				}
			}.run();
		} catch (Throwable e) { // NOSONAR
			return new Fail(e);
		}

		Statement statement = methodInvoker2(method, test, notifier);
		statement = possiblyExpectingExceptions(method, test, statement);
		statement = withPotentialTimeout(method, test, statement);
		statement = this.withBefores(method, test, statement, notifier);
		statement = this.withAfters(method, test, statement, notifier);
		statement = withRules(method, test, statement);
		return statement;
	}

	private Statement withRules(FrameworkMethod method, Object target, Statement statement) {
		List<TestRule> testRules = getTestRules(target);
		Statement result = statement;
		result = withMethodRules(method, testRules, target, result);
		result = withTestRules(method, testRules, result);
		return result;
	}

	private Statement withMethodRules(FrameworkMethod method, List<TestRule> testRules, Object target, Statement result) {
		for (org.junit.rules.MethodRule each : getMethodRules(target)) {
			if (!testRules.contains(each))
				result = each.apply(result, method, target);
		}
		return result;
	}

	private List<org.junit.rules.MethodRule> getMethodRules(Object target) {
		return rules(target);
	}

	private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules, Statement statement) {
		return testRules.isEmpty() ? statement : new RunRules(statement, testRules, describeChild(method));
	}

	@Override
	protected List<TestRule> getTestRules(Object target) {
		List<TestRule> result = getTestClass().getAnnotatedMethodValues(target, Rule.class, TestRule.class);
		result.addAll(getTestClass().getAnnotatedFieldValues(target, Rule.class, TestRule.class));
		return result;
	}

	@Override
	protected Statement classBlock(final RunNotifier notifier) {
		Statement statement = childrenInvoker(notifier);
		statement = this.withBeforeClasses(statement, notifier);
		statement = this.withAfterClasses(statement, notifier);
		statement = withClassRules(statement);
		return statement;
	}

	private Statement withClassRules(Statement statement) {
		List<TestRule> classRules = classRules();
		return classRules.isEmpty() ? statement : new RunRules(statement, classRules, getDescription());
	}

	@Override
	protected List<TestRule> classRules() {
		List<TestRule> result = fTestClass.getAnnotatedMethodValues(null, ClassRule.class, TestRule.class);
		result.addAll(fTestClass.getAnnotatedFieldValues(null, ClassRule.class, TestRule.class));
		return result;
	}
}
