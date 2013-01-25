package ca.cutterslade.util.processpool;

import java.util.concurrent.Callable;

public interface ProcessCallable<T> extends Callable<T> {

  ProcessConfiguration getProcessConfiguration();
}
