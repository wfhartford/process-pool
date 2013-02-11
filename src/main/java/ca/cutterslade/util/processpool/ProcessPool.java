package ca.cutterslade.util.processpool;

import java.util.concurrent.Callable;

import org.apache.commons.pool.KeyedObjectPool;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

class ProcessPool implements AutoCloseable {

  private final KeyedObjectPool<JvmFactory<?>, ProcessWrapper> pool;
  private final int port;

  ProcessPool(final KeyedObjectPool<JvmFactory<?>, ProcessWrapper> pool, final int port) {
    this.pool = pool;
    this.port = port;
  }

  public ProcessWrapper getWrapper(final JvmFactory<?> jvmFactory) {
    try {
      return pool.borrowObject(jvmFactory);
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  public void returnWrapper(final JvmFactory<?> jvmFactory, final ProcessWrapper wrapper) {
    try {
      pool.returnObject(jvmFactory, wrapper);
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  @Override
  public void close() {
    try {
      pool.close();
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  public JvmFactory<?> getJvmFactory(final Callable<?> callable, final JvmFactory<?> defaultJvmFactory) {
    final JvmFactory<?> base =
        callable instanceof SpecifiesJvmFactory ? ((SpecifiesJvmFactory) callable).getJvmFactory() : defaultJvmFactory;
    return base.clearProgram()
        .setMainClass(ProcessTask.class)
        .build();
  }
}
