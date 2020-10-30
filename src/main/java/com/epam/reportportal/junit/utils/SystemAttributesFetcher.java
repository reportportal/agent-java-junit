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
		Set<ItemAttributesRQ> systemAttributes = SystemAttributesExtractor.extract(PROPS_FILE,
				SystemAttributesFetcher.class.getClassLoader()
		);
		systemAttributes.add(skippedIssue(skippedAnIssue));
		return systemAttributes;
	}
}
