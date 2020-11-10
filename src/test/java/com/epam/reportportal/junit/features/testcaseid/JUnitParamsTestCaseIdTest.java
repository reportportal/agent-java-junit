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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class JUnitParamsTestCaseIdTest {

	public static final String TEST_CASE_ID_VALUE = "test-case-id";

	@TestCaseId(value = TEST_CASE_ID_VALUE, parametrized = true)
	@Test
	@Parameters({ "one, 1" })
	public void testParameters(@TestCaseIdKey String strParameter, int intParameter) {
		System.out.println("Parameter test: " + strParameter + "; " + intParameter);
	}
}
