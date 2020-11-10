/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.junit.features.parameters;

import com.epam.reportportal.annotations.ParameterKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TwoStandardParametersNoInterfaceTest {

	@Parameters
	public static Object[][] params() {
		return new Object[][] { new Object[] { "one", 1 }, new Object[] { "two", 2 } };
	}

	private final String strParameter;
	private final int intParameter;

	public TwoStandardParametersNoInterfaceTest(@ParameterKey("param1") String strParameter, @ParameterKey("param2") int intParameter) {
		this.strParameter = strParameter;
		this.intParameter = intParameter;
	}

	@Test
	public void testParameters() {
		System.out.println("Parameter test: " + strParameter + "; " + intParameter);
	}
}
