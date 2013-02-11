package ca.cutterslade.util.processpool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.ListenableFuture;

final class ProcessWrapperListenableFuture<T> implements ListenableFuture<T> {
  private final ProcessWrapperCallable<T> callable;
  private final ListenableFuture<T> future;

  ProcessWrapperListenableFuture(@Nonnull final ProcessWrapperCallable<T> callable,
      @Nonnull final ListenableFuture<T> future) {
    this.callable = callable;
    this.future = future;
  }

  @Override
  public void addListener(@Nonnull final Runnable listener, @Nonnull final Executor executor) {
    future.addListener(listener, executor);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return future.cancel(false) || callable.cancel(mayInterruptIfRunning);
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
  public T get(final long timeout, @Nonnull final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }
}
