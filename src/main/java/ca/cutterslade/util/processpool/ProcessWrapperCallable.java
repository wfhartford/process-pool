package ca.cutterslade.util.processpool;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

final class ProcessWrapperCallable<T> implements Callable<T> {
  private static final Logger log = LoggerFactory.getLogger(ProcessWrapperCallable.class);

  private final ProcessPool pool;
  private final Object mutex = new Object();
  private final JvmFactory jvmFactory;
  private final Callable<T> callable;
  private ProcessWrapper wrapper;
  private boolean cancelled;

  ProcessWrapperCallable(final ProcessPool pool, final JvmFactory defaultJvmFactory, final Callable<T> callable) {
    this.pool = pool;
    this.jvmFactory = pool.getJvmFactory(callable, defaultJvmFactory);
    this.callable = callable;
  }

  @Override
  public T call() throws Exception {
    final T result;
    synchronized (mutex) {
      if (cancelled) {
        throw new CancellationException();
      }
      wrapper = pool.getWrapper(jvmFactory);
    }
    try {
      result = wrapper.run(callable);
    }
    finally {
      pool.returnWrapper(jvmFactory, wrapper);
    }
    return result;
  }

  boolean cancel(final boolean mayInterruptIfRunning) {
    synchronized (mutex) {
      if (null == wrapper) {
        cancelled = true;
      }
      else if (mayInterruptIfRunning) {
        try {
          wrapper.kill();
        }
        catch (InterruptedException e) {
          log.warn("Interrupted killing process", e);
          Thread.currentThread().interrupt();
        }
      }
      return null == wrapper || mayInterruptIfRunning;
    }
  }
}
