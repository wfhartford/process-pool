package ca.cutterslade.util.processpool;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.cutterslade.util.jvmbuilder.JvmFactory;

final class ProcessWrapperFactory implements KeyedPoolableObjectFactory<JvmFactory<?>, ProcessWrapper> {
  private static final Logger log = LoggerFactory.getLogger(ProcessWrapperFactory.class);

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
