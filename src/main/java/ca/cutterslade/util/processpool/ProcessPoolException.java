package ca.cutterslade.util.processpool;

public class ProcessPoolException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  ProcessPoolException(final Throwable cause) {
    super(cause);
  }
}
