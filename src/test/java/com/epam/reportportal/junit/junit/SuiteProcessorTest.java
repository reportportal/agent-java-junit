/*
 * This file is part of Report Portal.
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.junit.junit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.SuitesKeeper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;

public class SuiteProcessorTest {

	@Test
	public void testPassedSuites() {
		SuitesKeeper suiteProcessor = new SuitesKeeper();
		suiteProcessor.addToSuiteKeeper(Description.createSuiteDescription(getClass()));
		Set<Class<?>> passedTests = new HashSet<Class<?>>();
		passedTests.add(this.getClass());
		Assert.assertTrue(suiteProcessor.isSuitePassed(this.getClass().getName(), passedTests));
	}

	@Test
	public void testNull() {
		SuitesKeeper suiteProcessor = new SuitesKeeper();
		suiteProcessor.addToSuiteKeeper(null);
		suiteProcessor.getSuiteName(null);
		suiteProcessor.isSuitePassed(null, null);
	}

	@Test
	public void testAddToKeeper() {
		SuitesKeeper suiteProcessor = new SuitesKeeper();
		suiteProcessor.addToSuiteKeeper(Description.createSuiteDescription(getClass()));
		Assert.assertEquals(getClass().getName(), suiteProcessor.getSuiteName(getClass()));
	}

	@Test
	public void testPropFile() throws FileNotFoundException, IOException {
		try {
			@SuppressWarnings("unused")
            ReportPortalListener listener = new ReportPortalListener();
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}

	}

}
