package exceptions;

public class InvalidIpException extends Exception {
  public InvalidIpException(String ip) {
    super("Invalid IP address: " + ip);
  }
}
