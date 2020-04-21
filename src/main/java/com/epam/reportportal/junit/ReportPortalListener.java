/*
 * Copyright 2018 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.junit;

import com.epam.reportportal.listeners.Statuses;
import com.nordstrom.automation.junit.AtomicTest;
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.MethodWatcher;
import com.nordstrom.automation.junit.RunWatcher;
import com.nordstrom.automation.junit.RunnerWatcher;
import com.nordstrom.automation.junit.ShutdownListener;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 *
 * @author Aliaksei_Makayed (modified by Andrei_Ramanchuk)
 */
public class ReportPortalListener implements ShutdownListener, RunnerWatcher, RunWatcher<FrameworkMethod>, MethodWatcher<FrameworkMethod> {

    private static final IListenerHandler HANDLER;

    static {
        HANDLER = new ParallelRunningHandler(new ParallelRunningContext());
        HANDLER.startLaunch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown() {
        HANDLER.stopLaunch();
    }

    @Override
    public void runStarted(Object runner) {
        HANDLER.startRunner(runner);
    }

    @Override
    public void runFinished(Object runner) {
        HANDLER.stopRunner(runner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(AtomicTest<FrameworkMethod> atomicTest) {
        HANDLER.startTest(atomicTest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(AtomicTest<FrameworkMethod> atomicTest) {
        HANDLER.finishTest(atomicTest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(AtomicTest<FrameworkMethod> atomicTest, Throwable thrown) {
        HANDLER.finishTest(atomicTest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(AtomicTest<FrameworkMethod> atomicTest, AssumptionViolatedException thrown) {
        HANDLER.finishTest(atomicTest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(AtomicTest<FrameworkMethod> atomicTest) {
        HANDLER.handleTestSkip(atomicTest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
        // if this is a JUnit configuration method
        if (HANDLER.isReportable(method)) {
            HANDLER.startTestMethod(runner, method, callable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable, Throwable thrown) {
        // if this is a JUnit configuration method
        if (HANDLER.isReportable(method)) {
            // if has exception
            if (thrown != null) {
                Class<? extends Throwable> expected = None.class;

                // if this is not a class-level configuration method
                if ((null == method.getAnnotation(BeforeClass.class)) &&
                    (null == method.getAnnotation(AfterClass.class))) {

                    AtomicTest<FrameworkMethod> atomicTest = LifecycleHooks.getAtomicTestOf(runner);
                    FrameworkMethod identity = atomicTest.getIdentity();
                    Test annotation = identity.getAnnotation(Test.class);
                    if (annotation != null) {
                        expected = annotation.expected();
                    }
                }

                if (!expected.isInstance(thrown)) {
                    reportTestFailure(callable, thrown);
                }
            }

            HANDLER.stopTestMethod(runner, method, callable);
        }
    }

    /**
     * Report failure of the indicated "particle" method.
     *
     * @param callable {@link ReflectiveCallable} object being intercepted
     * @param thrown exception thrown by method
     */
    public void reportTestFailure(ReflectiveCallable callable, Throwable thrown) {
        HANDLER.sendReportPortalMsg(callable, thrown);
        HANDLER.markCurrentTestMethod(callable, Statuses.FAILED);
    }

    @Override
    public Class<FrameworkMethod> supportedType() {
        return FrameworkMethod.class;
    }
}
