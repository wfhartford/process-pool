package ca.cutterslade.util.processpool;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class ProcessPoolExecutorService implements ListeningExecutorService {

  private final ListeningExecutorService executorService;

  ProcessPoolExecutorService(final ListeningExecutorService executorService) {
    this.executorService = executorService;
  }

  @Nonnull
  @Override
  public <T> ListenableFuture<T> submit(@Nonnull Callable<T> task) {
    return executorService.submit(new ProcessWrapperCallable<>(task));
  }

  @Nonnull
  @Override
  public <T> ListenableFuture<T> submit(@Nonnull Runnable task, T result) {
    return submit(Executors.callable(task, result));
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> submit(@Nonnull Runnable task) {
    return submit(task, null);
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return executorService.invokeAll(Collections2.transform(tasks, ProcessWrapperCallable.<T>wrapper()));
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout,
      @Nonnull TimeUnit unit) throws InterruptedException {
    return executorService.invokeAll(Collections2.transform(tasks, ProcessWrapperCallable.<T>wrapper()), timeout, unit);
  }

  @Nonnull
  @Override
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return executorService.invokeAny(Collections2.transform(tasks, ProcessWrapperCallable.<T>wrapper()));
  }

  @Nonnull
  @Override
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(Collections2.transform(tasks, ProcessWrapperCallable.<T>wrapper()), timeout, unit);
  }

  @Override
  public void execute(@javax.annotation.Nonnull final Runnable command) {
    submit(command);
  }

  @Override
  public void shutdown() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  @Nonnull
  public List<Runnable> shutdownNow() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean isShutdown() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean isTerminated() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
