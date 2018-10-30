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
