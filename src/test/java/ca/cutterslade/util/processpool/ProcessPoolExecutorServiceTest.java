package ca.cutterslade.util.processpool;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private static final String HELLO_WORLD = "Hello, World!";

  private enum TrueCallable implements Callable<Boolean>, Serializable {
    INSTANCE;
    private static final long serialVersionUID = 1L;

    @Override
    public Boolean call() throws Exception {
      return Boolean.TRUE;
    }
  }

  private enum HelloWorldCallable implements Callable<String>, Serializable {
    INSTANCE;
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws Exception {
      return HELLO_WORLD;
    }
  }

  private enum VmNameCallable implements Callable<String>, Serializable {
    INSTANCE;
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws Exception {
      return ManagementFactory.getRuntimeMXBean().getName();
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
    Assert.assertTrue(service.awaitTermination(10, TimeUnit.SECONDS));
  }

  @Test
  public void testSimpleCallable() throws InterruptedException, ExecutionException, TimeoutException {
    final ListenableFuture<Boolean> submit = service.submit(TrueCallable.INSTANCE);
    Assert.assertTrue(submit.get(10, TimeUnit.SECONDS));
  }

  @Test
  public void testHelloCallable() throws InterruptedException, ExecutionException, TimeoutException {
    final ListenableFuture<String> submit = service.submit(HelloWorldCallable.INSTANCE);
    Assert.assertEquals(HELLO_WORLD, submit.get(10, TimeUnit.SECONDS));
  }

  @Test
  public void testSerialExecutionsReuseVm()
      throws ExecutionException, TimeoutException, InterruptedException {
    final String myVmName = ManagementFactory.getRuntimeMXBean().getName();
    final ListenableFuture<String> vmName = service.submit(VmNameCallable.INSTANCE);
    final String pooledVmName = vmName.get(10, TimeUnit.SECONDS);
    Assert.assertNotEquals(myVmName, pooledVmName);
    final String nextPooledVmName = service.submit(VmNameCallable.INSTANCE).get(10, TimeUnit.SECONDS);
    Assert.assertEquals(pooledVmName, nextPooledVmName);
  }

  @Test
  public void testConcurrentExecutionsUseDifferentVm() throws InterruptedException, TimeoutException,
      ExecutionException {
    final String myVmName = ManagementFactory.getRuntimeMXBean().getName();
    final List<Future<String>> futures =
        service.invokeAll(Arrays.asList(VmNameCallable.INSTANCE, VmNameCallable.INSTANCE));
    Assert.assertEquals(2, futures.size());
    final String firstName = futures.get(0).get(10, TimeUnit.SECONDS);
    final String secondName = futures.get(1).get(10, TimeUnit.SECONDS);
    Assert.assertNotEquals(myVmName, firstName);
    Assert.assertNotEquals(myVmName, secondName);
    Assert.assertNotEquals(firstName, secondName);
  }
}
