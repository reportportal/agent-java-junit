package com.epam.reportportal.junit.utils;

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class SystemAttributesFetcherTest {

	private static List<String> expectedKeys = Lists.newArrayList("os", "agent", "skippedIssue");

	@Test
	public void systemAttributesFetchingTest() {
		Set<ItemAttributesRQ> systemAttributes = SystemAttributesFetcher.collectSystemAttributes(false);
		assertNotNull(systemAttributes);
		assertTrue(CollectionUtils.isNotEmpty(systemAttributes));
		systemAttributes.stream().map(ItemAttributesRQ::isSystem).forEach(Assert::assertTrue);
		assertTrue(systemAttributes.stream().map(ItemAttributesRQ::getKey).collect(Collectors.toList()).containsAll(expectedKeys));
		assertEquals(expectedKeys.size(), systemAttributes.stream().map(ItemAttributesRQ::getValue).filter(Objects::nonNull).count());
	}
}