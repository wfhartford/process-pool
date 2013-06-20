package ca.cutterslade.util.processpool;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.cutterslade.util.jvmbuilder.JvmFactory;
import ca.cutterslade.util.jvmbuilder.JvmFactoryBuilder;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class ProcessPoolExecutorService implements ListeningExecutorService {
  private static final Logger log = LoggerFactory.getLogger(ProcessPoolExecutorService.class);

  private static final ThreadFactory SHUTDOWN_THREAD_FACTORY =
      new ThreadFactoryBuilder().setNameFormat("ProcessPoolExecutorService-shutdown-thread-%d").build();

  private final class ShutdownRunnable implements Runnable {
    @Override
    public void run() {
      try {
        log.debug("Waiting for main executor to terminate");
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        log.debug("Main executor terminated");
      }
      catch (InterruptedException e) {
        log.warn("Interrupted waiting for executor service to terminate", e);
        Thread.currentThread().interrupt();
      }
      finally {
        log.debug("Closing process pool");
        pool.close();
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private final Function processWrapperFunction = new Function<Callable<?>, ProcessWrapperCallable<?>>() {
    @Nullable
    @Override
    public ProcessWrapperCallable<?> apply(@Nullable final Callable<?> input) {
      return new ProcessWrapperCallable<>(pool, defaultJvmFactory, input);
    }
  };
  private final ExecutorService shutdownService = Executors.newSingleThreadExecutor(SHUTDOWN_THREAD_FACTORY);
  private final ListeningExecutorService executorService;
  private final ProcessPool pool;
  private final JvmFactory<?> defaultJvmFactory;

  ProcessPoolExecutorService(final ListeningExecutorService executorService, final ProcessPool pool,
      final JvmFactoryBuilder<?> defaultJvmFactory) {
    this(executorService, pool, defaultJvmFactory.setMainClass(ProcessTask.class).build());
  }

  ProcessPoolExecutorService(final ListeningExecutorService executorService, final ProcessPool pool,
      final JvmFactory<?> defaultJvmFactory) {
    this.executorService = executorService;
    this.pool = pool;
    this.defaultJvmFactory = defaultJvmFactory;
    shutdownService.submit(new ShutdownRunnable());
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
    try {
      executorService.shutdown();
    }
    finally {
      shutdownService.shutdown();
    }
  }

  @Override
  @Nonnull
  public List<Runnable> shutdownNow() {
    final List<Runnable> runnables;
    try {
      runnables = executorService.shutdownNow();
    }
    finally {
      shutdownService.shutdown();
    }
    // It doesn't seem right to return these, since I've wrapped the user's requests, and the underlying service has as
    // well, but the documentation is unclear, and exactly what is in the list depends on the implementation of
    // executorService, which is up to the user. Any attempt to run what's returned will probably fail miserably, since
    // the runnables depend on the process pool, which will be shutdown shortly.
    return runnables;
  }

  @Override
  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return shutdownService.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException {
    return shutdownService.awaitTermination(timeout, unit);
  }

}
