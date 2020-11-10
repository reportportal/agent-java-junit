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

package com.epam.reportportal.junit.features.nested;

import com.epam.reportportal.annotations.Step;
import org.junit.Test;

public class NestedStepMultiLevelTest {

	public static final String METHOD_WITH_INNER_METHOD_NAME_TEMPLATE = "I am method with inner method";
	public static final String INNER_METHOD_NAME_TEMPLATE = "I am - {method}";

	@Test
	public void test() {
		methodWithInnerMethod();
	}

	@Step(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE)
	public void methodWithInnerMethod() {
		innerMethod();
	}

	@Step(INNER_METHOD_NAME_TEMPLATE)
	public void innerMethod() {
	}
}
