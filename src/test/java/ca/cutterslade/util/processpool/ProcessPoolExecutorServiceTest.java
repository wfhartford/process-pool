package ca.cutterslade.util.processpool;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.cutterslade.util.jvmbuilder.sun.SunJvmFactoryBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ProcessPoolExecutorServiceTest {
  private enum TrueCallable implements Callable<Boolean>, Serializable {
    INSTANCE;
    private static final long serialVersionUID = 1L;

    @Override
    public Boolean call() throws Exception {
      return Boolean.TRUE;
    }
  }

  private ProcessPoolExecutorService service;

  @Before
  public void setupService() {
    final ListeningExecutorService underlying = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    final ProcessPool pool = new ProcessPool(new KeyedObjectPoolProvider().get());
    service = new ProcessPoolExecutorService(underlying, pool, new SunJvmFactoryBuilder());
  }

  @After
  public void shutdownService() throws InterruptedException {
    service.shutdown();
    Assert.assertTrue(service.awaitTermination(10000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testSimpleCallable() throws InterruptedException, ExecutionException, TimeoutException {
    final ListenableFuture<Boolean> submit = service.submit(TrueCallable.INSTANCE);
    Assert.assertTrue(submit.get(10000, TimeUnit.MILLISECONDS));
  }
}
