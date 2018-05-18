package com.epam.reportportal.junit.junit;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.epam.reportportal.junit.ReportPortalListener;

public class UnitTestRunner extends BlockJUnit4ClassRunner {

	public UnitTestRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}
	
	@Override
	public void run(RunNotifier notifier) {
        notifier.addListener(new ReportPortalListener());
        notifier.fireTestRunStarted(getDescription());
        super.run(notifier);
	}

}
