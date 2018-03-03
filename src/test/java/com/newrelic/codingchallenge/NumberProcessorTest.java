package com.newrelic.codingchallenge;

import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import org.junit.Assert;
import org.junit.Test;

public class NumberProcessorTest {

  StatProcessor mockStatProcessor = mock(StatProcessor.class);
  Socket mockSocket = mock(Socket.class);

  @Test
  public void testTerminate() {
    String terminate = "terminate";
    when(mockStatProcessor.add(anyInt())).thenReturn(true);
    NumberProcessor numberProcessor = new NumberProcessor(mockSocket, mockStatProcessor);

    try {
      numberProcessor.processData(terminate);
      fail("Expected an exception to be thrown here");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof TerminateServerException);
    }
  }

  @Test
  public void testNumberFormat() {
    String number = "102032052";
    when(mockStatProcessor.add(anyInt())).thenReturn(true);
    NumberProcessor numberProcessor = new NumberProcessor(mockSocket, mockStatProcessor);

    try {
      numberProcessor.processData(number);
    } catch (Exception e) {
      fail("Expected testNumberFormat method to succeed");
    }

    String[] invalidNumbers = new String[]{"0120asda1", "1231", "012312313121", "-123123123"};
    for (String invalidNumber : invalidNumbers) {
      try {
        numberProcessor.processData(invalidNumber);
        fail("Should've failed with invalid data: " + invalidNumber);
      } catch (Exception e) {
        Assert.assertTrue(e instanceof IOException);
      }
    }

  }
}
