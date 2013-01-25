package ca.cutterslade.util.processpool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;

final class ProcessWrapperListenableFuture<T> implements ListenableFuture<T> {
  private final ProcessWrapperCallable<T> callable;
  private final ListenableFuture<T> future;

  ProcessWrapperListenableFuture(final ProcessWrapperCallable<T> callable, final ListenableFuture<T> future) {
    this.callable = callable;
    this.future = future;
  }

  @Override
  public void addListener(final Runnable listener, final Executor executor) {
    future.addListener(listener, executor);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    final boolean success;
    if (future.cancel(false) || callable.cancelIfNotStarted()) {
      success = true;
    }
    else if (mayInterruptIfRunning) {
      success = callable.abort();
    }
    else {
      success = false;
    }
    return success;
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public T get(final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get();
  }
}
