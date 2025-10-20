package models;

public class RouteEntry {
  public String networkAddress;
  public String subnetMask;
  public String nextHopIp; // IP do próximo salto (gateway)
  public int outputInterfaceIndex; // Índice no array 'interfaces' do Router

  // Construtor e Getters
  public RouteEntry(String net, String mask, String next, int index) {
    this.networkAddress = net;
    this.subnetMask = mask;
    this.nextHopIp = next;
    this.outputInterfaceIndex = index;
  }
}