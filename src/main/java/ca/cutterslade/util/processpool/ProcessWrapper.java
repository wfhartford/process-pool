package ca.cutterslade.util.processpool;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessWrapper implements Closeable {
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

  private static final String PING_RESPONSE = "pong";
  private static final ProcessCommand KILL_COMMAND = new ProcessCommand() {
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(final ProcessContext context) {
      context.killProcess();
    }
  };
  private static final Callable<String> PING_CALLABLE = new Callable<String>() {
    @Override
    public String call() throws Exception {
      return PING_RESPONSE;
    }
  };
  private final Process process;
  private final Socket socket;
  private final ObjectInput input;
  private final ObjectOutput output;
  private final AtomicBoolean running = new AtomicBoolean();

  ProcessWrapper(final Process process, final Socket socket) throws IOException {
    this.process = process;
    this.socket = socket;
    this.input = new ObjectInputStream(socket.getInputStream());
    this.output = new ObjectOutputStream(socket.getOutputStream());
  }

  void kill() throws InterruptedException {
    if (running.get()) {
      process.destroy();
    }
    else {
      try {
        writeCommand(KILL_COMMAND);
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
      writeCommand(new ExecuteCommand(callable));
      final Object result = readResult();
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

  private void writeCommand(final ProcessCommand processCommand) throws IOException {
    output.writeObject(processCommand);
  }

  private Object readResult() throws IOException, ClassNotFoundException {
    return input.readObject();
  }

  void ping() throws Exception {
    final String response = run(PING_CALLABLE);
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

