package ca.cutterslade.util.processpool;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProcessWrapper implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(ProcessWrapper.class);

  private static final class ThrowableResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Throwable throwable;

    ThrowableResult(final Throwable throwable) {
      this.throwable = throwable;
    }

    Throwable getThrowable() {
      return throwable;
    }
  }

  private static final class ExecuteCommand implements ProcessCommand {
    private static final long serialVersionUID = 1L;
    private final Callable<?> callable;

    private ExecuteCommand(final Callable<?> callable) {
      this.callable = callable;
    }

    @Override
    public void execute(final ProcessContext context) {
      Object result = null;
      try {
        result = callable.call();
      }
      catch (Throwable e) {
        result = new ThrowableResult(e);
        if (e instanceof Error) {
          throw (Error) e;
        }
      }
      finally {
        context.setResult(result);
      }
    }
  }

  private enum PingCallable implements Callable<String>, Serializable {
    INSTANCE;
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws Exception {
      return PING_RESPONSE;
    }
  }

  private static final String PING_RESPONSE = "pong";
  private static final ProcessCommand KILL_COMMAND = new ProcessCommand() {
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(final ProcessContext context) {
      context.killProcess();
    }
  };
  private final Process process;
  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;
  private final AtomicBoolean running = new AtomicBoolean();

  ProcessWrapper(final Process process, final Socket socket) throws IOException {
    this.process = process;
    this.socket = socket;
    try {
      this.input = socket.getInputStream();
      this.output = socket.getOutputStream();
    }
    catch (Throwable t) {
      socket.close();
      throw t;
    }
  }

  void kill() throws InterruptedException {
    if (running.get()) {
      process.destroy();
    }
    else {
      try {
        StreamUtils.writeObject(output, KILL_COMMAND);
      }
      catch (IOException e) {
        log.warn("Exception writing kill command, destroying process", e);
        process.destroy();
      }
    }
    process.waitFor();
  }

  <T> T run(final Callable<T> callable) throws ExecutionException {
    running.set(true);
    try {
      StreamUtils.writeObject(output, new ExecuteCommand(callable));
      final Object result = StreamUtils.readObject(input);
      if (result instanceof ThrowableResult) {
        throw new ExecutionException(((ThrowableResult) result).getThrowable());
      }
      return (T) result;
    }
    catch (IOException | ClassNotFoundException e) {
      throw new ExecutionException(e);
    }
    finally {
      running.set(false);
    }
  }

  void ping() throws Exception {
    final String response = run(PingCallable.INSTANCE);
    if (!PING_RESPONSE.equals(response)) {
      throw new IllegalStateException("Expected ping response of " + PING_RESPONSE + "; recieved " + response);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      kill();
    }
    catch (InterruptedException e) {
      log.warn("Interrupted while killing process", e);
      Thread.currentThread().interrupt();
    }
    finally {
      socket.close();
    }
  }

}

