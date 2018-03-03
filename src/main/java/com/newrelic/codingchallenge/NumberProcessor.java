package com.newrelic.codingchallenge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The basic processor thread that is responsible for handling individual client connections.
 * This class is provided with a socket that refers to a connected client.
 * The statProcessor is used to keep track of numbers that have been read already and thus
 * display the statistics.
 */
public class NumberProcessor {

  private static final Logger logger = LoggerFactory.getLogger(NumberProcessor.class);

  // Simple regex to match valid input. Any sequence of 9 digits.
  private static final String NUMBER_REGEX = "^\\d{9}$";

  private final Socket socket;
  private final StatProcessor statProcessor;

  public NumberProcessor(Socket socket, StatProcessor statProcessor) {
    this.socket = socket;
    this.statProcessor = statProcessor;
  }

  /**
   * The main processing function in the application.
   * Reads a line from the connected client and verifies if the line corresponds to the
   * given rules
   * - Is a 9 digit number
   * - Is a terminate command
   *
   * Each number is parsed as an integer and is stored using the statProcessor.
   * If the number is unique and has not been seen before, then it is also logged to a file.
   *
   * If the input doesn't correspond to the specified rules, the client is disconnected.
   *
   * @throws IOException If there is any issue with the client's connection or on invalid input
   * @throws TerminateServerException Thrown when the terminate command is provided by the client
   */
  public void process() throws IOException, TerminateServerException {
    // do the fun stuff here
    try (InputStream inputStream = socket.getInputStream()) {
      BufferedReader clientReader = new BufferedReader(new InputStreamReader(inputStream));
      String inputData;
      while ((inputData = clientReader.readLine()) != null) {
        processData(inputData);
      }
    }
  }

  public void processData(String inputData) throws TerminateServerException, IOException {
    if (inputData.equals("terminate")) {
      // We've received the terminate server command. Lets throw an exception and
      // let the caller thread handle this case
      throw new TerminateServerException("Termination command received");
    } else {
      if (!inputData.matches(NUMBER_REGEX)) {
        throw new IOException("Invalid number provided: " + inputData);
      }
      // Try to parse the input data as a long value
      Integer number = Integer.parseUnsignedInt(inputData);

      if (statProcessor.add(number)) {
        logger.info("{}", number);
      }

    }
  }
}
