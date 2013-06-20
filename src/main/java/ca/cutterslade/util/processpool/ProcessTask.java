package ca.cutterslade.util.processpool;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class ProcessTask implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(ProcessTask.class);

  public static void main(@Nonnull final String[] args) throws IOException, ClassNotFoundException {
    log.debug("Starting slave process");
    Preconditions.checkArgument(1 == args.length);
    final int port = Integer.parseInt(args[0]);
    try (ProcessTask task = new ProcessTask(port)) {
      task.executeCommands();
    }
    log.debug("Slave process main() exiting");
  }

  private final ProcessContext context = new ProcessContext() {
    @Override
    public void killProcess() {
      killed = true;
    }

    @Override
    public void setResult(final Object result) {
      ProcessTask.this.result = result;
    }
  };

  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;
  private boolean killed;
  private Object result;

  public ProcessTask(final int port) throws IOException {
    boolean success = false;
    this.socket = new Socket(InetAddress.getLocalHost(), port);
    try {
      this.input = socket.getInputStream();
      this.output = socket.getOutputStream();
      success = true;
    }
    finally {
      if (!success) {
        this.socket.close();
      }
    }
  }

  private void executeCommands() throws IOException, ClassNotFoundException {
    log.debug("Starting command loop");
    while (!killed) {
      final ProcessCommand command = readCommand();
      log.debug("Recieved command {}", command);
      command.execute(context);
      writeResult();
    }
  }

  private void writeResult() throws IOException {
    log.debug("Writing command result {}", result);
    StreamUtils.writeObject(output, result);
  }

  private ProcessCommand readCommand() throws IOException, ClassNotFoundException {
    return (ProcessCommand) StreamUtils.readObject(input);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
