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

import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.annotations.attribute.MultiValueAttribute;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class MultiValueAttributeTest {
	private static final String VALUE_1 = "v1";
	private static final String VALUE_2 = "v2";

	public static final Collection<Pair<String, String>> ATTRIBUTES = Arrays.asList(Pair.of(null, VALUE_1),
			Pair.of(null, VALUE_2));


	@Test
	@Attributes(multiValueAttributes = { @MultiValueAttribute(isNullKey = true, values = { VALUE_1, VALUE_2 }) })
	public void third() {
		System.out.println("Test class: " + getClass().getCanonicalName());
	}
}
