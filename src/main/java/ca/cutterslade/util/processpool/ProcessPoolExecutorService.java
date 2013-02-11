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
import javax.annotation.Nullable;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public final class ProcessPoolExecutorService implements ListeningExecutorService {

  @SuppressWarnings("rawtypes")
  private final Function processWrapperFunction = new Function<Callable<?>, ProcessWrapperCallable<?>>() {
    @Nullable
    @Override
    public ProcessWrapperCallable<?> apply(@Nullable final Callable<?> input) {
      return new ProcessWrapperCallable<>(pool, defaultJvmFactory, input);
    }
  };
  private final ListeningExecutorService executorService;
  private final ProcessPool pool;
  private final JvmFactory<?> defaultJvmFactory;

  ProcessPoolExecutorService(final ListeningExecutorService executorService, final ProcessPool pool,
      final JvmFactory<?> defaultJvmFactory) {
    this.executorService = executorService;
    this.pool = pool;
    this.defaultJvmFactory = defaultJvmFactory;
  }

  private <T> ProcessWrapperCallable<T> wrapper(final Callable<T> task) {
    return this.<T>wrapperFunction().apply(task);
  }

  private <T> Function<Callable<T>, ProcessWrapperCallable<T>> wrapperFunction() {
    return (Function<Callable<T>, ProcessWrapperCallable<T>>) processWrapperFunction;
  }

  @Nonnull
  @Override
  public <T> ListenableFuture<T> submit(@Nonnull final Callable<T> task) {
    return executorService.submit(wrapper(task));
  }

  @Nonnull
  @Override
  public <T> ListenableFuture<T> submit(@Nonnull final Runnable task, final T result) {
    return submit(callable(task, result));
  }

  private <T, R extends SpecifiesJvmFactory & Runnable> Callable<T> callable(final Runnable task, final T result) {
    //noinspection CastConflictsWithInstanceof
    return task instanceof SpecifiesJvmFactory ?
        new JvmCallable<>((R) task, result) :
        Executors.callable(task, result);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> submit(@Nonnull final Runnable task) {
    return submit(task, null);
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull final Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return executorService.invokeAll(Collections2.transform(tasks, this.<T>wrapperFunction()));
  }

  @Nonnull
  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull final Collection<? extends Callable<T>> tasks, final long timeout,
      @Nonnull final TimeUnit unit) throws InterruptedException {
    return executorService.invokeAll(Collections2.transform(tasks, this.<T>wrapperFunction()), timeout, unit);
  }

  @Nonnull
  @Override
  public <T> T invokeAny(@Nonnull final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return executorService.invokeAny(Collections2.transform(tasks, this.<T>wrapperFunction()));
  }

  @Nonnull
  @Override
  public <T> T invokeAny(@Nonnull final Collection<? extends Callable<T>> tasks, final long timeout, @Nonnull
  final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(Collections2.transform(tasks, this.<T>wrapperFunction()), timeout, unit);
  }

  @Override
  public void execute(@Nonnull final Runnable command) {
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
  public boolean awaitTermination(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
