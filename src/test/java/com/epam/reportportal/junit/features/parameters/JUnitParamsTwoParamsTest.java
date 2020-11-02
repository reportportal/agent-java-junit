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

import com.google.common.base.Optional;
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

@RunWith(JUnitParamsRunner.class)
public class JUnitParamsTwoParamsTest implements ArtifactParams {

	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

	@Test
	@Parameters({ "one, 1", "two, 2" })
	public void testParameters(String param1, int param2) {
		System.out.println("Parameter test: " + param1 + "; " + param2);
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
		Object runner = LifecycleHooks.getRunnerForTarget(this);
		AtomicTest<?> test = LifecycleHooks.getAtomicTestOf(runner);
		ReflectiveCallable callable = LifecycleHooks.getCallableOf(runner, test.getIdentity());
		try {
			Object[] params = LifecycleHooks.getFieldValue(callable, "val$params");
			return Param.mapOf(Param.param("param1", params[0]), Param.param("param2", params[1]));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			return Optional.absent();
		}
	}
}
