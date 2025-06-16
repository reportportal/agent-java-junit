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
import com.epam.reportportal.annotations.attribute.MultiKeyAttribute;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class MultipleAttributeTest {
	private static final String KEY_1 = "key1";
	private static final String KEY_2 = "key2";
	private static final String KEY_3 = "k1";
	private static final String KEY_4 = "k2";

	private static final String VALUE_1 = "value1";
	private static final String VALUE_2 = "value2";
	private static final String VALUE_3 = "v";
	private static final String VALUE_4 = VALUE_3;

	public static final Collection<Pair<String, String>> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
			Pair.of(KEY_1, VALUE_1),
			Pair.of(KEY_2, VALUE_2),
			Pair.of(KEY_3, VALUE_3),
			Pair.of(KEY_4, VALUE_4)
	));

	@Test
	@Attributes(attributes = { @Attribute(key = KEY_1, value = VALUE_1), @Attribute(key = KEY_2, value = VALUE_2) }, multiKeyAttributes = {
			@MultiKeyAttribute(keys = { KEY_3, KEY_4 }, value = VALUE_3) })
	public void second() {
		System.out.println("Test class: " + getClass().getCanonicalName());
	}
}
