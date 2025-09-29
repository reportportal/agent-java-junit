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

package com.epam.reportportal.junit.features.callback;

import com.epam.reportportal.junit.ParallelRunningContext;
import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.utils.ItemTreeUtils;
import com.epam.reportportal.service.tree.ItemTreeReporter;
import com.epam.reportportal.service.tree.TestItemTree;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CallbackLogFeatureTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CallbackLogFeatureTest.class);

	public static final String ERROR_LOG_LEVEL = "Error";
	public static final String LOG_MESSAGE = "Error log message";
	public static final Instant LOG_TIME = Instant.now();

	@Test
	public void someTest() {
		Assert.assertEquals(1, 1);
	}

	@After
	public void afterMethod() {
		TestItemTree tree = ParallelRunningContext.getCurrent().getItemTree();
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(getClass().getCanonicalName() + ".someTest", tree);
		if (testItemLeaf != null) {
			attachLog(tree, testItemLeaf);
		} else {
			LOGGER.error("Callback leaf not found");
		}
	}

	private static void attachLog(TestItemTree tree, TestItemTree.TestItemLeaf testItemLeaf) {
		ItemTreeReporter.sendLog(
				ReportPortalListener.getReportPortal().getClient(),
				ERROR_LOG_LEVEL,
				LOG_MESSAGE,
				LOG_TIME,
				tree.getLaunchId(),
				testItemLeaf
		);
	}

}
