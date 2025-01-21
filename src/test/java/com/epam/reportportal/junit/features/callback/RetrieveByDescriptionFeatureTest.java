/*
 * Copyright 2021 EPAM Systems
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
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class RetrieveByDescriptionFeatureTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveByDescriptionFeatureTest.class);
	private static final List<TestItemTree.TestItemLeaf> testItemLeaves = new ArrayList<>();

	public static final String SLID = "SLID";
	public static final String SLID_VALUE = "0586c1c90fcd4a499591109692426d54";

	@Rule
	public TestRule rule = new TestWatcher() {
		@Override
		protected void finished(@Nonnull Description description) {
			ofNullable(ItemTreeUtils.retrieveLeaf(
					description,
					ParallelRunningContext.getCurrent().getItemTree()
			)).ifPresent(testItemLeaves::add);
		}
	};

	@Test
	public void stepWithSauceLabsAttribute() {
		LOGGER.info("SauceLabs job id will be added as attribute in after class method");
	}

	@AfterClass
	public static void afterClass() {
		addSaucelabsAttribute();
	}

	private static void addSaucelabsAttribute() {
		FinishTestItemRQ request = new FinishTestItemRQ();
		request.setEndTime(Calendar.getInstance().getTime());
		request.setStatus("PASSED");
		request.setAttributes(Collections.singleton(new ItemAttributesRQ(SLID, SLID_VALUE)));
		ItemTreeReporter.finishItem(
				ReportPortalListener.getReportPortal().getClient(),
				request,
				ParallelRunningContext.getCurrent().getItemTree().getLaunchId(),
				testItemLeaves.get(0)
		).cache().ignoreElement().blockingAwait();
	}
}
