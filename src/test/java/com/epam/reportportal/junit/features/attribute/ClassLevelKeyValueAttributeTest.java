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

package com.epam.reportportal.junit.features.attribute;

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import org.junit.Test;

@Attributes(attributes = { @Attribute(key = ClassLevelKeyValueAttributeTest.KEY, value = ClassLevelKeyValueAttributeTest.VALUE) })
public class ClassLevelKeyValueAttributeTest {

	public static final String KEY = "attribute_test_key";
	public static final String VALUE = "attribute_test_value";

	@Test
	public void first() {
		System.out.println("Test class: " + getClass().getCanonicalName());
	}
}
