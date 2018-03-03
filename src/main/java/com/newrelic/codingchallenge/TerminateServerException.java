package com.newrelic.codingchallenge;

/**
 * Custom exception to handle the server termination command.
 */
public class TerminateServerException extends Exception {
  public TerminateServerException(String msg) {
    super(msg);
  }
}
