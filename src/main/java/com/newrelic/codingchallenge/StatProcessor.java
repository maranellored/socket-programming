package com.newrelic.codingchallenge;

import com.google.common.collect.Sets;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the statistics from the number processor thread.
 */
public class StatProcessor {

  private static final Logger logger = LoggerFactory.getLogger(StatProcessor.class);

  private final Set<Integer> allNumbers;
  private int lastKnownSize = 0;

  // FIXME: Can this be a regular int?
  private final AtomicInteger duplicateCount;

  public StatProcessor() {
    this.allNumbers = Sets.newConcurrentHashSet();
    this.duplicateCount = new AtomicInteger(0);
  }

  /**
   * Gets the count of duplicate numbers seen and sets it to 0 atomically.
   *
   * @return
   */
  public int getAndResetDuplicateCountSinceLastRun() {
    return duplicateCount.getAndSet(0);
  }


  public int getAllUniqueCount() {
    return allNumbers.size();
  }

  public int getUniqueCountSinceLastRun() {
    int currentSize = allNumbers.size();
    int uniqueCount = currentSize - lastKnownSize;
    lastKnownSize = currentSize;

    return uniqueCount;
  }

  /**
   * Add a new number to our list of unique numbers.
   * If the number is unique i.e. it didn't exist in our list previously,
   * then we update the unique count that we've soon so far.
   * If not, then we update the duplicates count.
   *
   * @param number
   * @return if the added number is unique or not
   */
  public boolean add(Integer number) {
    boolean isUnique = allNumbers.add(number);

    if (!isUnique) {
      duplicateCount.incrementAndGet();
    }

    return isUnique;
  }

  /**
   * This method is scheduled to run on a set schedule.
   */
  public void run() {
    int duplicateCount = getAndResetDuplicateCountSinceLastRun();
    int uniqueCount = getUniqueCountSinceLastRun();

    // Print the value to stdout.
    logger.info("Received {} unique numbers, {} duplicates. Unique total: {}",
        uniqueCount, duplicateCount, lastKnownSize);
  }
}
