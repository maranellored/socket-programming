# coding-challenge

New Relic Senior Software Engineer Coding Challenge Build Framework

## Starter build framework for the coding challenge

First, you do not need to use this starter framework for your project.
If you would rather use a different build system (maven, javac, ...)
you are free to so long as you provide clear commands to build your
project and start your server.  Failure to do so will invalidate your
submission.


## Install Java

This coding challenge is in Java so it is recommended you install Java
1.8 from Oracle.


## Gradle

The build framework provided here uses gradle to build your project
and manage your dependencies.  The `gradlew` command used here will
automatically download gradle for you so you shouldn't need to install
anything other than java.


### Project Layout

All source code should be located in the `src/main/java` folder.
If you wish to write any tests (not a requirement) they should be
located in the `src/test/java` folder.

A starter `Main.java` file has been provided in the `com/newrelic/codingchallenge` package under `src/main/java`.


### Dependencies

If your project has any dependencies you can list them in the
`build.gradle` file in the `dependencies` section.


### Building your project from the command line

To build the project on Linux or MacOS run the command `./gradlew build` in a shell terminal.  This will build the source code in
`src/main/java`, run any tests in `src/test/java` and create an output
jar file in the `build/libs` folder.

To clean out any intermediate files run `./gradlew clean`.  This will
remove all files in the `build` folder.


### Running your application from the command line

You first must create a shadow jar file.  This is a file which contains your project code and all dependencies in a single jar file.  To build a shadow jar from your project run `./gradlew shadowJar`.  This will create a `codeing-challenge-shadow.jar` file in the `build/libs` directory.

You can then start your application by running the command
`java -jar ./build/lib/coding-challenge-shadow.jar`

## IDEA

You are free to use whichever editor or IDE you want providing your
projects build does not depend on that IDE.  Most of the Java
developers at New Relic use IDEA from
[JetBrains](https://www.jetbrains.com/).  JetBrains provides
a community edition of IDEA which you can download and use without
charge.

If you are planning to use IDEA you can generate the IDEA project files
by running `./gradlew idea` and directly opening the project folder
as a project in idea.

## Implementation Details and Assumptions
* This implementation uses Blocking I/O instead of NIO. The reason for that is because the number of clients is bounded and this number should be schedulable on a modern CPU without the need for extensive context switching. 

  It might be possible to achieve much higher throughput with NIO but the tradeoff in terms of complexity of an NIO implementation and in the interest of time, the accuracy of the implementation with a blocking IO solution was preferred.
  
* Uses Logback as the logging library to log to a file. The default appender just prints to the STDOUT. 
  But the `NumberProcessor` class has a specially configured appender to log to a file on disk, as mentioned in the requirements. 

  Another huge throughput gain in the application, could be by preventing the appender from Flushing to disk immediately, which is the default.
  The logback configuration file is placed in the `src/main/resources` directory. 

* Since the valid range of numbers is from 0 to 999,999,999. The total number of unique values is a billion. 

  This allows us to store the numbers as Integers in memory in order to check for duplicates. 
  In the worst case, this would require at least 4G of memory for the JVM. 
  This can easily be switched to a `Long`, if larger statistics are to be provided.
 
* In order to handle synchronization issues, this implementation uses a ConcurrentHashSet implementation. For space benefits, this could be replaced by a BitSet. 

  The use of a BitSet would entail custom synchronization logic to ensure thread safety.   

* The handling of the duplicate number count uses an `AtomicInteger` to safe guard against concurrency issues. 
  This could be another bottleneck in the performance of the application. 
  
* The entire implementation assumes that the counts provided by the statistics reporter are highly accurate. 
  By relaxing this assumption, it could be possible to achieve higher throughput by allowing for weaker concurrency primitives. 
  
* Since the input numbers are parsed as Integers, leading 0s are dropped when they are logged to the file and stored internally.

* The error messages if any, are logged to STDOUT. 

* Keeps track of all the open connections in a ConcurrentHashSet since there are thread safety concerns around multiple clients trying to close a connection at the same time. 
  Also uses a semaphore to keep track of the number of connected clients. 

* The implementation has been tested with an `Xmx` and `Xms` both set to 2G. 
  Higher starting heap size should ideally provide for better throughput since the JVM wouldn't need to call into the kernel every time. 
  
* The server's newline sequence is either a line feed `\n`, a carriage return `\r` or a carriage return followed by a line feed, as defined in the Java standard library.

* The number of connections is limited to 5 as defined in the requirements. 
  
  If a new client tries to connect after the server has reached its maximum number of connections, then the connection is closed with no real message. 
 
* Uses a fixed size thread pool to handle the client connections, and also single scheduled executor to display the stats periodically.
