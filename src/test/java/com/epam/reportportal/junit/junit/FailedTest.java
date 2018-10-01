package com.epam.reportportal.junit.junit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FailedTest {

    @Test
    public void testFailureWithCustomMessage() {
        Assert.assertEquals("Failure msg", 2, 1);
    }

    @Test
    public void testFailure() {
        Assert.assertEquals(2, 1);
    }
    
    @Test
    @Ignore
    public void testIgnore() {
    	Assert.assertEquals(1,  1);
    }
}
