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

package com.epam.reportportal.junit.features.retry;

import com.nordstrom.automation.junit.ArtifactParams;
import com.nordstrom.automation.junit.AtomIdentity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Parameterized.class)
public class StandardParametersRetryTest implements ArtifactParams {
	public static final int FAILURE_NUMBER = 1;
	public static final String FAILURE_PARAMETER = "one";
	public static final AtomicInteger RETRY_COUNT = new AtomicInteger(0);

	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

	@Parameters
	public static Object[] params() {
		return new Object[] { "one", "two, three" };
	}

	private final String parameter;

	public StandardParametersRetryTest(String param) {
		parameter = param;
	}

	@Test
	public void testParameters() {
		if (FAILURE_PARAMETER.equals(parameter)) {
			if (RETRY_COUNT.getAndIncrement() < FAILURE_NUMBER) {
				throw new IllegalArgumentException("Retry error");
			}
		}
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
		return Optional.of(Collections.singletonMap("param", parameter));
	}
}
