package ca.cutterslade.util.processpool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

final class ProcessWrapperFactory implements KeyedPoolableObjectFactory<JvmFactory<?>, ProcessWrapper> {
  private static final Logger log = LoggerFactory.getLogger(ProcessWrapperFactory.class);

  private static final ThreadFactory THREAD_FACTORY =
      new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ProcessWrapperFactory-output-reader-%d").build();

  private static final class ReaderRunnable implements Runnable {
    private final InputStream stream;
    private final PrintStream output;

    private ReaderRunnable(final InputStream stream, final PrintStream output) {
      this.stream = stream;
      this.output = output;
    }

    @Override
    public void run() {
      try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
        for (String line = reader.readLine(); null != line; line = reader.readLine()) {
          output.println(line);
        }
      }
      catch (IOException e) {
        log.warn("Exception reading from child's stream");
      }
    }
  }

  private final ExecutorService readerExecutorService = Executors.newCachedThreadPool(THREAD_FACTORY);

  private final int acceptTimeout;

  private final int readTimeout;

  ProcessWrapperFactory(final int acceptTimeout, final int readTimeout) {
    this.acceptTimeout = acceptTimeout;
    this.readTimeout = readTimeout;
  }

  @Override
  public ProcessWrapper makeObject(final JvmFactory<?> key) throws Exception {
    Process process = null;
    Socket socket = null;
    boolean success = false;
    try (final ServerSocket server = new ServerSocket(0, 0, InetAddress.getLocalHost())) {
      server.setSoTimeout(acceptTimeout);
      final int port = server.getLocalPort();
      process = key.start(String.valueOf(port));
      readerExecutorService.submit(new ReaderRunnable(process.getInputStream(), System.out));
      readerExecutorService.submit(new ReaderRunnable(process.getErrorStream(), System.err));
      socket = server.accept();
      socket.setSoTimeout(readTimeout);
      success = true;
    }
    finally {
      if (!success) {
        try {
          if (null != socket) {
            socket.close();
          }
        }
        finally {
          if (null != process) {
            process.destroy();
          }
        }
      }
    }
    return new ProcessWrapper(process, socket);
  }

  @Override
  public void destroyObject(final JvmFactory<?> key, final ProcessWrapper obj) throws Exception {
    obj.close();
  }

  @Override
  public boolean validateObject(final JvmFactory<?> key, final ProcessWrapper obj) {
    boolean valid = false;
    try {
      obj.ping();
      valid = true;
    }
    catch (Exception e) {
      log.warn("Exception thrown validating wrapper {}", obj, e);
    }
    return valid;
  }

  @Override
  public void activateObject(final JvmFactory<?> key, final ProcessWrapper obj) throws Exception {
  }

  @Override
  public void passivateObject(final JvmFactory<?> key, final ProcessWrapper obj) throws Exception {
  }
}
