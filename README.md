Process Pool
============
This project is an implementation of the standard Java [ExecutorService](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html) interface (actually guava's [ListeningExecutorService](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListeningExecutorService.html)) which uses seperate JVM processes rather than threads to execute tasks.