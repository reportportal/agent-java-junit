/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.junit;

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.TestCaseIdKey;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.nordstrom.automation.junit.ArtifactParams;
import com.nordstrom.automation.junit.AtomIdentity;
import com.nordstrom.automation.junit.AtomicTest;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class ParallelRunningHandlerTest {

	private static final List<String> EXPECTED_KEYS = Lists.newArrayList("jvm", "os", "agent", "skippedIssue");
	private final ParallelRunningContext parallelRunningContext = mock(ParallelRunningContext.class);
	private final ParallelRunningHandler parallelRunningHandler = new ParallelRunningHandler(parallelRunningContext);
	private final ListenerParameters listenerParameters = TestUtils.standardParameters();

	@Test
	public void buildStartLaunchRqWithSystemAttributesTest() {
		StartLaunchRQ startLaunchRQ = parallelRunningHandler.buildStartLaunchRq(listenerParameters);
		Set<ItemAttributesRQ> systemAttributes = startLaunchRQ.getAttributes();
		assertNotNull(systemAttributes);
		assertTrue(CollectionUtils.isNotEmpty(systemAttributes));
		systemAttributes.stream().map(ItemAttributesRQ::isSystem).forEach(Assertions::assertTrue);
		assertTrue(systemAttributes.stream().map(ItemAttributesRQ::getKey).collect(Collectors.toList()).containsAll(EXPECTED_KEYS));
		assertEquals(EXPECTED_KEYS.size(), systemAttributes.stream().map(ItemAttributesRQ::getValue).filter(Objects::nonNull).count());
	}

	@Test
	public void shouldReturnProvidedIdWhenNotParametrized() throws NoSuchMethodException {

		Method methodForTesting = this.getClass().getDeclaredMethod("methodForTesting");

		@SuppressWarnings("unchecked")
		AtomicTest<FrameworkMethod> test = mock(AtomicTest.class);
		String codeRef = "simpleCodeRef";

		TestCaseIdEntry testCaseId = parallelRunningHandler.getTestCaseId(methodForTesting, test, codeRef);

		assertEquals("testId", testCaseId.getId());
	}

	@Test
	public void shouldReturnProvidedIdWhenParametrized() throws NoSuchMethodException {

		Method methodForTesting = this.getClass().getDeclaredMethod("shouldReturnProvidedIdWhenParametrized");

		DummyTest dummyTest = new DummyTest();
		String codeRef = "simpleCodeRef";

		TestCaseIdEntry testCaseId = parallelRunningHandler.getTestCaseId(methodForTesting, dummyTest, codeRef);

		assertEquals("I am test id", testCaseId.getId());
	}

	@Test
	public void shouldReturnGeneratedIdWhenParametrizedAndKeyNotProvided() throws NoSuchMethodException {

		Method methodForTesting = this.getClass().getDeclaredMethod("shouldReturnProvidedIdWhenParametrized");

		DummyTestWithoutKey dummyTest = new DummyTestWithoutKey();
		String codeRef = "simpleCodeRef";

		TestCaseIdEntry testCaseId = parallelRunningHandler.getTestCaseId(methodForTesting, dummyTest, codeRef);

		assertEquals(codeRef, testCaseId.getId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithKey() {

		DummyTest dummyTest = new DummyTest();
		String codeRef = "simpleCodeRef";

		TestCaseIdEntry testCaseId = parallelRunningHandler.retrieveParametrizedTestCaseId(dummyTest,
				dummyTest.getParameters().get(),
				codeRef
		);

		assertEquals("I am test id", testCaseId.getId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithoutKey() {

		DummyTestWithoutKey dummyTestWithoutKey = new DummyTestWithoutKey();
		String codeRef = "simpleCodeRef";

		TestCaseIdEntry testCaseId = parallelRunningHandler.retrieveParametrizedTestCaseId(dummyTestWithoutKey,
				dummyTestWithoutKey.getParameters().get(),
				codeRef
		);

		assertEquals(codeRef, testCaseId.getId());
	}

	@TestCaseId(value = "testId")
	public void methodForTesting() {

	}

	public static class DummyTest implements ArtifactParams {
		
		@org.junit.Rule
		public final AtomIdentity identity = new AtomIdentity(this);

		@TestCaseIdKey
		private String testId = "I am test id";

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
			Map<String, Object> map = new HashMap<>();
			map.put("testId", testId);
			return Optional.of(map);
		}

		@org.junit.Test
		public void method() {

		}
	}

	public static class DummyTestWithoutKey implements ArtifactParams {

		@org.junit.Rule
		public final AtomIdentity identity = new AtomIdentity(this);

		private String testId = "I am test id";

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
			Map<String, Object> map = new HashMap<>();
			map.put("testId", testId);
			return Optional.of(map);
		}

		@org.junit.Test
		public void method() {

		}
	}
}