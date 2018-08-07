package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class DummyTest {
    
    static {
        System.out.println("<init> AppTest");
    }
    
    @Before
    public void before() throws InterruptedException {
    	Thread.sleep(1000);
    }
    
    @Test
    public void test() throws InterruptedException {
    	Thread.sleep(1000);
        assertTrue(true);
    }
    
    @After
    public void after() throws InterruptedException {
    	Thread.sleep(1000);
    }
    
}
