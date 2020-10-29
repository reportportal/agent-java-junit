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
	@Parameters({ "one", "two\\, three" })
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
		Object runner = LifecycleHooks.getRunnerForTarget(this);
		AtomicTest<?> test = LifecycleHooks.getAtomicTestOf(runner);
		ReflectiveCallable callable = LifecycleHooks.getCallableOf(runner, test.getIdentity());
		try {
			Object[] params = LifecycleHooks.getFieldValue(callable, "val$params");
			String param = (String) params[0];
			return Param.mapOf(Param.param("param", param));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			return Optional.absent();
		}
	}
}
