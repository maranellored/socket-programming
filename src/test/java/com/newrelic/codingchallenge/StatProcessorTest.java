package com.newrelic.codingchallenge;

import org.junit.Assert;
import org.junit.Test;

public class StatProcessorTest {

  @Test
  public void testAdd() {
    StatProcessor processor = new StatProcessor();

    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(processor.add(i));
    }

    Assert.assertEquals(10, processor.getAllUniqueCount());
    Assert.assertEquals(10, processor.getUniqueCountSinceLastRun());

    Assert.assertEquals(0, processor.getUniqueCountSinceLastRun());
    Assert.assertEquals(0, processor.getAndResetDuplicateCountSinceLastRun());

    for (int i = 0; i < 5; i++) {
      Assert.assertFalse(processor.add(i));
    }

    Assert.assertEquals(10, processor.getAllUniqueCount());
    Assert.assertEquals(0, processor.getUniqueCountSinceLastRun());
    Assert.assertEquals(5, processor.getAndResetDuplicateCountSinceLastRun());

    processor.add(10);
    Assert.assertEquals(1, processor.getUniqueCountSinceLastRun());
    Assert.assertEquals(11, processor.getAllUniqueCount());
  }

}
