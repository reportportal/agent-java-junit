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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.epam.reportportal.service.ReportPortal;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Dzmitry_Kavalets
 */
public class JUnitProvider implements Provider<IListenerHandler> {

	@Inject
	private ParallelRunningContext parallelRunningContext;

	private ReportPortal reportPortalService;
	
	@Inject
	public void setReportPortalService() {
		this.reportPortalService = ReportPortal.builder().build();
	}

	@Override
	public IListenerHandler get() {

		if (reportPortalService.getParameters().getEnable()) {
			return new ParallelRunningHandler(parallelRunningContext, reportPortalService);
		}

		return (IListenerHandler) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { IListenerHandler.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});
	}
}
