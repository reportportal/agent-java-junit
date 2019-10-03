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

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.tree.TestItemTree;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Dzmitry_Kavalets
 */
public class JUnitProvider implements Provider<IListenerHandler> {

	@Inject
	private ParallelRunningContext parallelRunningContext;

	public static ReportPortal REPORT_PORTAL = ReportPortal.builder().build();
	public static ThreadLocal<TestItemTree> ITEM_TREE = new InheritableThreadLocal<TestItemTree>() {
		@Override
		protected TestItemTree initialValue() {
			return new TestItemTree();
		}
	};

	@Override
	public IListenerHandler get() {
		if (REPORT_PORTAL.getParameters().getEnable()) {
			return new ParallelRunningHandler(parallelRunningContext, REPORT_PORTAL);
		}

		return (IListenerHandler) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { IListenerHandler.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Class<?> returnType = method.getReturnType();

						if (ClassUtils.isAssignable(returnType, Boolean.class, true)) {
							return false;
						}

						return null;
					}
				}
		);
	}
}
