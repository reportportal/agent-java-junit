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

package com.epam.reportportal.junit.features.beforeafter;

import com.epam.reportportal.annotations.ParameterKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BeforeFailedParametrizedTest {

	@Parameters
	public static Object[] params() {
		return new Object[] { "one", "two, three" };
	}

	private final String parameter;

	public BeforeFailedParametrizedTest(@ParameterKey("param") String param) {
		parameter = param;
	}

	@Before
	public void beforeEachFailed() {
		throw new IllegalStateException("Before each");
	}

	@Test
	public void testBeforeEachFailed() {
		System.out.println("Test: testBeforeEachFailed - " + parameter);
	}
}
