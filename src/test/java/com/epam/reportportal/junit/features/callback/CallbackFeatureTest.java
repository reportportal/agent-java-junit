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
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class CallbackFeatureTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CallbackFeatureTest.class);

	public static final String ITEM_CALLBACK_FINISH_STATUS = "FAILED";

	@Test
	public void someTest() {
		Assertions.assertEquals(1, 1);
	}

	@After
	public void afterMethod() {
		TestItemTree tree = ParallelRunningContext.getCurrent().getItemTree();
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(getClass().getCanonicalName() + ".someTest",
				ParallelRunningContext.getCurrent().getItemTree()
		);
		if (testItemLeaf != null) {
			finishWithStatus(ITEM_CALLBACK_FINISH_STATUS, tree, testItemLeaf);
		} else {
			LOGGER.error("Callback leaf not found");
		}
	}

	private void finishWithStatus(String status, TestItemTree tree, TestItemTree.TestItemLeaf testItemLeaf) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status);
		finishTestItemRQ.setEndTime(Calendar.getInstance().getTime());
		ItemTreeReporter.finishItem(ReportPortalListener.getReportPortal().getClient(), finishTestItemRQ, tree.getLaunchId(), testItemLeaf)
				.cache()
				.blockingGet();
	}
}
