package models;

import java.net.InetAddress;
import java.net.UnknownHostException;
import exceptions.InvalidIpException;
import exceptions.InvalidMaskException;

public class NetworkInterface {

  private String ipAddress;
  private String subnetMask;
  private Device neighbor;

  public NetworkInterface(String ip, String mask) throws InvalidIpException, InvalidMaskException {
    setIpAddress(ip);
    setSubnetMask(mask);
  }

  public void setIpAddress(String ip) throws InvalidIpException {
    if (!isValidIP(ip))
      throw new InvalidIpException(ip);
    this.ipAddress = ip;
  }

  public void setSubnetMask(String mask) throws InvalidMaskException {
    if (!isValidMask(mask))
      throw new InvalidMaskException(mask);
    this.subnetMask = mask;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getSubnetMask() {
    return subnetMask;
  }

  public Device getNeighbor() {
    return neighbor;
  }

  public void setNeighbor(Device neighbor) {
    this.neighbor = neighbor;
  }

  public static boolean isValidIP(String ip) {
    try {
      InetAddress addr = InetAddress.getByName(ip);
      return addr.getHostAddress().equals(ip);
    } catch (UnknownHostException e) {
      return false;
    }
  }

  public static boolean isValidMask(String mask) {
    String[] parts = mask.split("\\.");
    if (parts.length != 4)
      return false;

    int maskInt = 0;
    for (String part : parts) {
      int octet;
      try {
        octet = Integer.parseInt(part);
      } catch (NumberFormatException e) {
        return false;
      }
      if (octet < 0 || octet > 255)
        return false;
      maskInt = (maskInt << 8) | octet;
    }

    boolean seenZero = false;
    for (int i = 31; i >= 0; i--) {
      boolean bit = (maskInt & (1 << i)) != 0;
      if (!bit)
        seenZero = true;
      else if (seenZero)
        return false;
    }
    return true;
  }

  public static boolean sameSubnet(String ip1, String mask, String ip2) {
    try {
      byte[] ipBytes1 = InetAddress.getByName(ip1).getAddress();
      byte[] ipBytes2 = InetAddress.getByName(ip2).getAddress();
      byte[] maskBytes = InetAddress.getByName(mask).getAddress();

      for (int i = 0; i < 4; i++) {
        if ((ipBytes1[i] & maskBytes[i]) != (ipBytes2[i] & maskBytes[i]))
          return false;
      }
      return true;
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
