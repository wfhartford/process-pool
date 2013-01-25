package ca.cutterslade.util.processpool;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import com.google.common.base.Function;

final class ProcessWrapperCallable<T> implements Callable<T> {
  private static final Function<Callable<?>, ProcessWrapperCallable<?>> WRAPPER_FUNCTION =
      new Function<Callable<?>, ProcessWrapperCallable<?>>() {
        @Nullable
        @Override
        public ProcessWrapperCallable<?> apply(@Nullable final Callable<?> callable) {
          return new ProcessWrapperCallable<>(callable);
        }
      };

  @SuppressWarnings("unchecked")
  static <T> Function<Callable<T>, ProcessWrapperCallable<T>> wrapper() {
    return (Function<Callable<T>, ProcessWrapperCallable<T>>) WRAPPER_FUNCTION;
  }

  private Callable<T> callable;

  ProcessWrapperCallable(Callable<T> callable) {
    this.callable = callable;
  }

  @Override
  public T call() throws Exception {
    throw new UnsupportedOperationException("not yet implemented");
  }

  boolean abort() {
  }

  boolean cancelIfNotStarted() {
  }
}
