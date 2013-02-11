package ca.cutterslade.util.processpool;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

public final class ProcessTask implements Closeable {

  public static void main(@Nonnull final String[] args) throws IOException, ClassNotFoundException {
    Preconditions.checkArgument(1 == args.length);
    final int port = Integer.parseInt(args[0]);
    try (ProcessTask task = new ProcessTask(port)) {
      task.executeCommands();
    }
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
  private final ObjectInput input;
  private final ObjectOutput output;
  private boolean killed;
  private Object result;

  public ProcessTask(final int port) throws IOException {
    boolean success = false;
    this.socket = new Socket(InetAddress.getLocalHost(), port);
    try {
      this.input = new ObjectInputStream(socket.getInputStream());
      this.output = new ObjectOutputStream(socket.getOutputStream());
      success = true;
    }
    finally {
      if (!success) {
        this.socket.close();
      }
    }
  }

  private void executeCommands() throws IOException, ClassNotFoundException {
    while (!killed) {
      final ProcessCommand command = readCommand();
      command.execute(context);
      writeResult();
    }
  }

  private void writeResult() throws IOException {
    output.writeObject(result);
  }

  private ProcessCommand readCommand() throws IOException, ClassNotFoundException {
    return (ProcessCommand) input.readObject();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
