package com.epam.reportportal.junit.utils;

import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;

import java.util.Set;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class SystemAttributesFetcher {

	private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	private static final String PROPS_FILE = "agent.properties";

	private SystemAttributesFetcher() {
		// static only
	}

	private static ItemAttributesRQ skippedIssue(Boolean skippedAnIssue) {
		ItemAttributesRQ skippedIssueAttr = new ItemAttributesRQ();
		skippedIssueAttr.setKey(SKIPPED_ISSUE_KEY);
		skippedIssueAttr.setValue(skippedAnIssue == null ? "true" : skippedAnIssue.toString());
		skippedIssueAttr.setSystem(true);
		return skippedIssueAttr;
	}

	public static Set<ItemAttributesRQ> collectSystemAttributes(Boolean skippedAnIssue) {
		Set<ItemAttributesRQ> systemAttributes = SystemAttributesExtractor.extract(PROPS_FILE);
		systemAttributes.add(skippedIssue(skippedAnIssue));
		return systemAttributes;
	}
}
