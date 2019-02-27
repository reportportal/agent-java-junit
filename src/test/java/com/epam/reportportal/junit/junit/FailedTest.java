package com.epam.reportportal.junit.junit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class FailedTest {

    @Test
    public void testFailureWithCustomMessage() {
        Assert.assertEquals("Failure msg", 2, 1);
    }

    @Test
    public void testFailure() {
        Assert.assertEquals(2, 1);
    }
    
    @Test(expected = AssertionError.class)
    public void expectedFailureThrown() {
        Assert.assertEquals(2, 1);
    }
    
    @Test(expected = AssertionError.class)
    public void expectedFailureAbsent() {
        Assert.assertEquals(1, 1);
    }
    
    @Test
    @Ignore
    public void testIgnore() {
    	Assert.assertEquals(1,  1);
    }
}
