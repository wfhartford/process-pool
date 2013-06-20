package ca.cutterslade.util.processpool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

enum StreamUtils {
  ;

  static void writeObject(final OutputStream output, final Object object) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (final ObjectOutputStream out = new ObjectOutputStream(baos)) {
      out.writeObject(object);
    }
    final byte[] bytes = baos.toByteArray();
    final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    lengthBuffer.putInt(bytes.length);
    lengthBuffer.flip();
    final byte[] lengthArray = new byte[4];
    lengthBuffer.get(lengthArray);
    output.write(lengthArray);
    output.write(bytes);
  }

  static Object readObject(final InputStream input) throws IOException, ClassNotFoundException {
    final byte[] lengthBytes = new byte[4];
    final int lengthRead = input.read(lengthBytes);
    if (4 != lengthRead) {
      throw new IOException("Expected 4 bytes but only read " + lengthRead);
    }
    final ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
    final byte[] resultBytes = new byte[lengthBuffer.getInt()];
    final int resultRead = input.read(resultBytes);
    if (resultBytes.length != resultRead) {
      throw new IOException("Expected " + resultBytes.length + " bytes but only read " + resultRead);
    }
    try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(resultBytes))) {
      return objectInputStream.readObject();
    }
  }
}
