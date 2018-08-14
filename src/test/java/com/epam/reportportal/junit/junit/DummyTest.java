package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class DummyTest {
    
    @Before
    public void before() throws InterruptedException {
    }
    
    @Test
    public void test1() throws InterruptedException {
        assertTrue(true);
    }
    
    @Test
    public void test2() throws InterruptedException {
        assertTrue(true);
    }
    
    @After
    public void after() throws InterruptedException {
    }
    
}
