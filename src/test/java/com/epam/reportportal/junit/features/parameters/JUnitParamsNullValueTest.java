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

import com.nordstrom.automation.junit.ArtifactParams;
import com.nordstrom.automation.junit.AtomIdentity;
import com.nordstrom.automation.junit.AtomicTest;
import com.nordstrom.automation.junit.LifecycleHooks;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Optional;

@RunWith(JUnitParamsRunner.class)
public class JUnitParamsNullValueTest implements ArtifactParams {

	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

	public static Object[] testParameters() {
		return new Object[] { "one", null };
	}

	@Test
	@Parameters(method = "testParameters")
	public void testParameters(String parameter) {
		System.out.println("Parameter test: " + parameter);
	}

	@Override
	public AtomIdentity getAtomIdentity() {
		return identity;
	}

	@Override
	public Description getDescription() {
		return identity.getDescription();
	}

	@Override
	public Optional<Map<String, Object>> getParameters() {
		AtomicTest test = LifecycleHooks.getAtomicTestOf(this);
		ReflectiveCallable callable = LifecycleHooks.getCallableOf(test.getDescription());
		try {
			Object[] params = LifecycleHooks.getFieldValue(callable, "val$params");
			String param = (String) params[0];
			return Param.mapOf(Param.param("param", param));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			return Optional.empty();
		}
	}
}
