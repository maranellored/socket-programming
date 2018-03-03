package com.newrelic.codingchallenge;


import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // Total number of concurrent tasks that can be run, which is the same as the total number of
  // clients that can be connected at any given time.
  private static final int MAX_CONCURRENT_TASKS = 5;

  // Setup the frequency at which the statistics are printed to the console
  private static final int STAT_FREQUENCY_SECONDS = 10;

  // The default socket to listen on
  private static final int DEFAULT_SOCKET = 4000;

  private final StatProcessor statProcessor;

  // A semaphore to keep track of the number of clients currently connected
  private final Semaphore semaphore;
  // A set to keep track of all the active client connections
  private final Set<Socket> activeClientSockets;

  // A boolean flag to control server shutdown
  private volatile boolean serverAlive = true;

  public Main() {
    this.statProcessor = new StatProcessor();
    this.activeClientSockets = Sets.newConcurrentHashSet();
    // A semaphore to keep track of the number of clients
    this.semaphore = new Semaphore(MAX_CONCURRENT_TASKS, true);
  }

  /**
   * The entrypoint to the application!
   * Kicks off a statProcessor thread that is scheduled to run every 10 seconds and print out
   * the necessary statistics.
   *
   * Then sets up a thread pool with at most {@link #MAX_CONCURRENT_TASKS} threads that is used
   * to service client requests.
   * The main thread is responsible for accepting new connections.
   *
   * FIXME: Can we get rid of the semaphore and just use the socket Set size to control number of clients?
   */
  public void start() {
    ScheduledExecutorService statService = Executors.newSingleThreadScheduledExecutor();
    // Schedule the statProcessor to run at a fixed rate
    statService.scheduleAtFixedRate(() -> statProcessor.run(), STAT_FREQUENCY_SECONDS,
        STAT_FREQUENCY_SECONDS, TimeUnit.SECONDS);

    // Fixed size thread pool to handle clients
    ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS);

    final ServerSocket serverSocket = createServerSocket();

    // Keep running until the server has been instructed to shut down
    while (serverAlive) {
      // Accept new connections.
      try {
        Socket clientSocket = serverSocket.accept();
        // Accepted a new connection. Check to see if we can process it.
        if (!semaphore.tryAcquire()) {
          // We've reached the maximum number of clients. Close this silently.
          clientSocket.close();
          continue;
        }

        activeClientSockets.add(clientSocket);
        // Submit to the pool, to spin off a new thread for handling requests
        executorService.submit(() -> processClient(clientSocket, serverSocket));
      } catch (IOException e) {
        // Client error, so continue on after logging error
        logger.error("Error processing client connection: {}", e.getMessage());
      }

    }

    // Shut down the executor pool since we are done.
    shutdownService(executorService);
    // Shut down the Stat service thread
    statService.shutdown();
    // Turn off the lights
    System.exit(0);

  }

  /**
   * Runs within the thread pool.
   * Instantiates a numberProcessor object and provides it with the newly connected client socket
   * to process the input.
   *
   * HACK: The server socket is used to shut down the server main thread when the terminate
   * command is sent, since the main thread could be blocked in an accept call.
   */
  private void processClient(Socket clientSocket, ServerSocket serverSocket) {
    NumberProcessor numberProcessor = new NumberProcessor(clientSocket, statProcessor);
    try {
      numberProcessor.process();
    } catch (IOException ioe) {
      // If the call has failed, then we need to close the socket.
      logger.error("Invalid input: {}", ioe.getMessage());
    } catch (TerminateServerException tse) {
      // We've received the terminate server command.
      // Set the serverAlive flag to false
      serverAlive = false;
    } finally {
      try {
        clientSocket.close();
        if (!serverAlive) {
          serverSocket.close();
        }
      } catch (IOException e) {
        // Nothing to be done here.
        logger.error("Failed to close socket: {}", e.getMessage());
      }
      // Remove the client socket from the active list and decrement our semaphore
      activeClientSockets.remove(clientSocket);
      semaphore.release();
    }
  }

  /**
   * Creates a server socket on the default port defined.
   * If the socket cannot be created, then the application exits completely, since
   * that is an irrecoverable error.
   *
   * @return Newly created server socket
   */
  private ServerSocket createServerSocket() {
    try {
      ServerSocket serverSocket = new ServerSocket(DEFAULT_SOCKET);
      return serverSocket;
    } catch (IOException e) {
      // Cannot open a port to listen to. Bail
      logger.error("Cannot open server port. Quitting");
      System.exit(-1);
    }

    return null;
  }

  /**
   * Tries to shut down the thread pool that was created to handle client requests.
   * If the service doesn't shut down cleanly, then it tries to close the open client
   * sockets manually.
   *
   * @param executorService The thread pool for clients
   */
  private void shutdownService(ExecutorService executorService) {
    // Be nice and try to shut down
    executorService.shutdown();

    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        // Pool did not shut down. Close the sockets manually
        for (Socket socket : activeClientSockets) {
          try {
            socket.close();
          } catch (IOException e) {
            // Failed to close the socket cleanly.
            // Nothing to do here. Bail and continue
            logger.error("Failed to close client socket while shutting down: {}", e.getMessage());
          }
        }
      }

    } catch (InterruptedException ie) {
      // Interrupted while waiting. Nothing to do here.
      logger.error("Interrupted while trying to shut down all clients");
    }

  }


  /**
   * The main function!!
   */
  public static void main(String[] args) {

    logger.info("Starting up server ....");
    // Add your code here
    new Main().start();
  }
}
