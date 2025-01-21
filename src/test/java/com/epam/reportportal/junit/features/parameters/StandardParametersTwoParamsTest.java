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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RunWith(Parameterized.class)
public class StandardParametersTwoParamsTest implements ArtifactParams {

	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

	@Parameters
	public static Object[][] params() {
		return new Object[][] { { "one", 1 }, { "two", 2 } };
	}

	private final String param1;
	private final int param2;

	public StandardParametersTwoParamsTest(String param1, int param2) {
		this.param1 = param1;
		this.param2 = param2;
	}

	@Test
	public void testParameters() {
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
		return Optional.of(new HashMap<String, Object>() {{
			put("param1", param1);
			put("param2", param2);
		}});
	}
}
