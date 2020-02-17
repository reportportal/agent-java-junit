package com.epam.reportportal.junit;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ParallelRunningHandlerTest {

	@Mock
	private ParallelRunningContext parallelRunningContext;

	@InjectMocks
	private ParallelRunningHandler parallelRunningHandler;

	private static ListenerParameters listenerParameters;

	private static List<String> expectedKeys = Lists.newArrayList("jvm", "os", "agent", "skippedIssue");

	@BeforeClass
	public static void beforeClass() throws Exception {
		listenerParameters = new ListenerParameters();
		listenerParameters.setLaunchName("test-launch");
	}

	@Test
	public void buildStartLaunchRqWithSystemAttributesTest() {
		StartLaunchRQ startLaunchRQ = parallelRunningHandler.buildStartLaunchRq(listenerParameters);
		Set<ItemAttributesRQ> systemAttributes = startLaunchRQ.getAttributes();
		assertNotNull(systemAttributes);
		assertTrue(CollectionUtils.isNotEmpty(systemAttributes));
		systemAttributes.stream().map(ItemAttributesRQ::isSystem).forEach(Assert::assertTrue);
		assertTrue(systemAttributes.stream().map(ItemAttributesRQ::getKey).collect(Collectors.toList()).containsAll(expectedKeys));
		assertEquals(expectedKeys.size(), systemAttributes.stream().map(ItemAttributesRQ::getValue).filter(Objects::nonNull).count());
	}
}