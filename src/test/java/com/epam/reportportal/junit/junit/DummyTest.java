package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class DummyTest {
	
	@BeforeClass
	public static void beforeClass() throws InterruptedException {
    	Thread.sleep(100);
	}
    
    @Before
    public void before() throws InterruptedException {
    	Thread.sleep(100);
    }
    
    @Test
    public void test1() throws InterruptedException {
    	Thread.sleep(100);
        assertTrue(true);
    }
    
    @Test
    public void test2() throws InterruptedException {
    	Thread.sleep(100);
        assertTrue(true);
    }
    
    @After
    public void after() throws InterruptedException {
    	Thread.sleep(100);
    }
    
	@AfterClass
	public static void afterClass() throws InterruptedException {
    	Thread.sleep(100);
	}
    
}
