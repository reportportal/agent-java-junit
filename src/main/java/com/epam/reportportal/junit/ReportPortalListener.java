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

import org.junit.Test;
import org.junit.Test.None;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;

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
    public void testStarted(AtomicTest atomicTest) {
        handler.startTestMethod(atomicTest.getIdentity(), atomicTest.getRunner());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(AtomicTest atomicTest) {
        handler.stopTestMethod(atomicTest.getIdentity(), atomicTest.getRunner());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(AtomicTest atomicTest, Throwable thrown) {
        reportTestFailure(atomicTest.getIdentity(), atomicTest.getRunner(), thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(AtomicTest atomicTest, AssumptionViolatedException thrown) {
        reportTestFailure(atomicTest.getIdentity(), atomicTest.getRunner(), thrown);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(AtomicTest atomicTest) {
        handler.handleTestSkip(atomicTest.getIdentity(), atomicTest.getRunner());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeInvocation(Object runner, Object target, FrameworkMethod method, Object... params) {
        if ((null == method.getAnnotation(Test.class)) && handler.isReportable(method)) {
            handler.startTestMethod(method, runner);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterInvocation(Object runner, Object target, FrameworkMethod method, Throwable thrown) {
        if ((null == method.getAnnotation(Test.class)) && handler.isReportable(method)) {
            if (thrown != null) {
                Class<? extends Throwable> expected = None.class;
                AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(runner);
                
                if (atomicTest != null) {
                    Test annotation = atomicTest.getIdentity().getAnnotation(Test.class);
                    expected = annotation.expected();
                }
                
                if (!expected.isInstance(thrown)) {
                    reportTestFailure(method, runner, thrown);
                }
            }

            handler.stopTestMethod(method, runner);
        }
    }

    /**
     * Report failure of the indicated "particle" method.
     * 
     * @param method {@link FrameworkMethod} object for the "particle" method
     * @throws RestEndpointIOException is something goes wrong
     */
    public void reportTestFailure(FrameworkMethod method, Object runner, Throwable thrown) {
        handler.sendReportPortalMsg(method, runner, thrown);
        handler.markCurrentTestMethod(method, runner, Statuses.FAILED);
    }

}
