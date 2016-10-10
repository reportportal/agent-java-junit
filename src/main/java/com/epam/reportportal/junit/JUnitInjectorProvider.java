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

import com.epam.reportportal.guice.Injector;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * JUnit provider injector is the child injector of
 * {@link com.epam.ta.reportportal.guice.Injector} with JUnits's specific
 * modules
 * 
 * @author Ilya_Koshaleu
 * 
 */
public class JUnitInjectorProvider {

	private static Supplier<Injector> instance = Suppliers.memoize(new Supplier<Injector>() {
		@Override
		public Injector get() {
			return Injector.getInstance().getChildInjector(new JUnitListenersModule());
		}
	});

	public static Injector getInstance() {
		return instance.get();
	}
}
