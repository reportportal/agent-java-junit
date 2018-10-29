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
