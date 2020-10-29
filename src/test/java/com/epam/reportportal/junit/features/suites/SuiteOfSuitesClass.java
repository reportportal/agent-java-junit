/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.junit.features.suites;

import com.epam.reportportal.junit.features.suites.tests.FirstSuiteTest;
import com.epam.reportportal.junit.features.suites.tests.SecondSuiteTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ SimpleSuiteClass.class })
public class SuiteOfSuitesClass {
}
