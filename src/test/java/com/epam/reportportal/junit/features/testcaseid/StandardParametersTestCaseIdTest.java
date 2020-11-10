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

package com.epam.reportportal.junit.features.testcaseid;

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.TestCaseIdKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StandardParametersTestCaseIdTest {

	public static final String TEST_CASE_ID_VALUE = "test-case-id";

	@Parameters
	public static Object[][] params() {
		return new Object[][] { new Object[] { "one", 1 } };
	}

	private final String strParameter;
	private final int intParameter;

	public StandardParametersTestCaseIdTest(@TestCaseIdKey String strParameter, int intParameter) {
		this.strParameter = strParameter;
		this.intParameter = intParameter;
	}

	@TestCaseId(value = TEST_CASE_ID_VALUE, parametrized = true)
	@Test
	public void testParameters() {
		System.out.println("Parameter test: " + strParameter + "; " + intParameter);
	}
}
