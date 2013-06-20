package ca.cutterslade.util.processpool;

import java.util.concurrent.Callable;

import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

final class ProcessPool implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(ProcessPool.class);
  private final KeyedObjectPool<JvmFactory<?>, ProcessWrapper> pool;

  ProcessPool(final KeyedObjectPool<JvmFactory<?>, ProcessWrapper> pool) {
    this.pool = pool;
  }

  public ProcessWrapper getWrapper(final JvmFactory<?> jvmFactory) {
    log.debug("Getting wrapper for {}", jvmFactory);
    try {
      return pool.borrowObject(jvmFactory);
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  public void returnWrapper(final JvmFactory<?> jvmFactory, final ProcessWrapper wrapper) {
    log.debug("Returning wrapper {} for {}", wrapper, jvmFactory);
    try {
      pool.returnObject(jvmFactory, wrapper);
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  @Override
  public void close() {
    log.debug("Closing");
    try {
      pool.close();
    }
    catch (Exception e) {
      throw new ProcessPoolException(e);
    }
  }

  public JvmFactory<?> getJvmFactory(final Callable<?> callable, final JvmFactory<?> defaultJvmFactory) {
    final JvmFactory<?> base = callable instanceof SpecifiesJvmFactory ?
        ((SpecifiesJvmFactory) callable).getJvmFactory() : defaultJvmFactory;
    log.debug("Getting JVM Factory for {} based on {}", callable, base);
    return base.clearProgram()
        .setMainClass(ProcessTask.class)
        .build();
  }
}
