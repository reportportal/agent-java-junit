package com.epam.reportportal.junit.features.parameters;

import com.epam.reportportal.annotations.ParameterKey;
import com.google.common.base.Optional;
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

@RunWith(Parameterized.class)
public class StandardParametersTwoParamsTest implements ArtifactParams {

	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

	@Parameters
	public static Object[] params() {
		return new Object[] { "one", "two, three" };
	}

	private final String parameter;

	public StandardParametersTwoParamsTest(@ParameterKey("param") String param) {
		parameter = param;
	}

	@Test
	public void testParameters() {
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
		return Optional.of(Collections.singletonMap("param", parameter));
	}
}
