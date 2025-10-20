package exceptions;

public class InvalidMaskException extends Exception {
  public InvalidMaskException(String mask) {
    super("Invalid subnet mask: " + mask);
  }
}
