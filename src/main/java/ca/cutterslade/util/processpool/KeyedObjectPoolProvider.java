package ca.cutterslade.util.processpool;

import javax.inject.Provider;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

class KeyedObjectPoolProvider implements Provider<KeyedObjectPool<JvmFactory<?>, ProcessWrapper>> {
  private final KeyedPoolableObjectFactory<JvmFactory<?>, ProcessWrapper> factory;
  private final int maxActive;
  private final byte whenExhaustedAction;
  private final long maxWait;
  private final int maxIdle;
  private final int maxTotal;
  private final int minIdle;
  private final boolean testOnBorrow;
  private final boolean testOnReturn;
  private final long timeBetweenEvictionRunsMillis;
  private final int numTestsPerEvictionRun;
  private final long minEvictableIdleTimeMillis;
  private final boolean testWhileIdle;
  private final boolean lifo;

  public KeyedObjectPoolProvider(final KeyedPoolableObjectFactory<JvmFactory<?>, ProcessWrapper> factory,
      final int maxActive, final byte whenExhaustedAction, final long maxWait, final int maxIdle, final int maxTotal,
      final int minIdle, final boolean testOnBorrow, final boolean testOnReturn,
      final long timeBetweenEvictionRunsMillis, final int numTestsPerEvictionRun, final long minEvictableIdleTimeMillis,
      final boolean testWhileIdle, final boolean lifo) {
    this.factory = factory;
    this.maxActive = maxActive;
    this.whenExhaustedAction = whenExhaustedAction;
    this.maxWait = maxWait;
    this.maxIdle = maxIdle;
    this.maxTotal = maxTotal;
    this.minIdle = minIdle;
    this.testOnBorrow = testOnBorrow;
    this.testOnReturn = testOnReturn;
    this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    this.testWhileIdle = testWhileIdle;
    this.lifo = lifo;
  }

  @Override
  public KeyedObjectPool<JvmFactory<?>, ProcessWrapper> get() {
    return new GenericKeyedObjectPool<>(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, minIdle,
        testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis,
        testWhileIdle, lifo);
  }
}
