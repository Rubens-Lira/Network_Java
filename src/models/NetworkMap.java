package models;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkMap {

  private static final int MAX_DEVICES = 100;
  private final Device[] devices;
  private int deviceCount = 0;
  private int nextDeviceId = 1000;

  private static final int MAX_PACKETS_IN_TRANSIT = 500;
  private final Packet[] packetsInTransit;
  private int packetCount = 0;

  private static final int ANIMATION_STEPS = 10;

  public NetworkMap() {
    this.devices = new Device[MAX_DEVICES];
    this.packetsInTransit = new Packet[MAX_PACKETS_IN_TRANSIT];
  }

  // --- Métodos de Gerenciamento e Busca de Dispositivos ---

  public boolean addDevice(Device d) {
    if (deviceCount >= MAX_DEVICES) {
      System.out.println("Error: Maximum device limit reached.");
      return false;
    }
    // Nota: A lógica de verificação de ID duplicado é correta, mas deve ser
    // revisitada se você tiver problemas de ID.
    /*
     * for (int i = 0; i < deviceCount; i++) {
     * if (devices[i].getId() == d.getId()) {
     * System.out.println("Error: Device with ID " + d.getId() +
     * " already exists.");
     * return false;
     * }
     * }
     */
    devices[deviceCount] = d;
    deviceCount++;
    return true;
  }

  // MODIFICADO: Cria e adiciona um novo dispositivo (usado pela GUI)
  public Device createAndAddDevice(String type, String name, String ip, String mask, int x, int y) throws Exception {
    Device newDevice = null;
    int newId = nextDeviceId++;

    if (type.equals("Host")) {
      // Calcula o gateway (simplificado)
      String gateway = getGatewayFromIpAndMask(ip, mask);
      newDevice = new Host(newId, name, x, y, gateway);
    } else if (type.equals("Router")) {
      newDevice = new Router(newId, name, x, y);
    }

    if (newDevice != null) {
      // 1. Cria e adiciona a primeira interface
      NetworkInterface intf = new NetworkInterface(ip, mask);
      newDevice.addInterface(intf);

      // 2. Adiciona o dispositivo ao mapa
      if (addDevice(newDevice)) {
        // LOG DE CONFIRMAÇÃO ATUALIZADO
        System.out.println("✅ CONFIRMAÇÃO: Adicionado " + newDevice.getName() +
            " (Contagem total: " + deviceCount + ") em (" + x + ", " + y + ")");
        return newDevice;
      } else {
        // LOG DE FALHA NA ADIÇÃO
        System.out.println("❌ FALHA: addDevice() retornou false para " + newDevice.getName());
      }
    }
    return null;
  }

  // NOVO: Determina um gateway padrão para o Host
  private String getGatewayFromIpAndMask(String ip, String mask) {
    try {
      byte[] ipBytes = InetAddress.getByName(ip).getAddress();
      byte[] maskBytes = InetAddress.getByName(mask).getAddress();
      byte[] netBytes = new byte[4];

      for (int i = 0; i < 4; i++) {
        netBytes[i] = (byte) (ipBytes[i] & maskBytes[i]);
      }

      // Define o último octeto da rede como 1 (Gateway Comum)
      netBytes[3] = 1;

      return InetAddress.getByAddress(netBytes).getHostAddress();

    } catch (UnknownHostException e) {
      return "0.0.0.0";
    }
  }

  public Device findDeviceByIP(String ip) {
    for (int i = 0; i < deviceCount; i++) {
      Device d = devices[i];
      for (int j = 0; j < d.getInterfaceCount(); j++) {
        if (d.getInterface(j).getIpAddress().equals(ip)) {
          return d;
        }
      }
    }
    return null;
  }

  public Device findDeviceByCoordinates(int x, int y, int tolerance) {
    for (int i = 0; i < deviceCount; i++) {
      Device d = devices[i];
      int distanceSq = (x - d.getX()) * (x - d.getX()) + (y - d.getY()) * (y - d.getY());

      if (distanceSq <= tolerance * tolerance) {
        return d;
      }
    }
    return null;
  }

  // --- Métodos de Gerenciamento de Pacotes ---

  public void startTransmission(String sourceIp, String destinationIp, int quantity) {
    Device source = findDeviceByIP(sourceIp);

    if (source == null) {
      System.out.println("Error: Source device not found for IP " + sourceIp);
      return;
    }

    for (int i = 0; i < quantity && packetCount < MAX_PACKETS_IN_TRANSIT; i++) {
      Packet p = new Packet(sourceIp, destinationIp, source);

      packetsInTransit[packetCount] = p;
      packetCount++;

      source.processPacket(p);
    }
    System.out.println("Attempting to send " + quantity + " packets from " + sourceIp);
  }

  // --- Motor de Simulação (Tick) ---

  public void tickSimulation() {
    // 1. Atualizar Posição Visual e Processar Chegada
    for (int i = 0; i < packetCount; i++) {
      Packet p = packetsInTransit[i];

      if (p == null || !p.isInTransit())
        continue;

      Device next = p.getNextHop();
      Device current = p.getCurrentDevice();

      if (next != null) {
        // Animação: Move 1/ANIMATION_STEPS do caminho a cada tick
        int dx = (next.getX() - current.getX()) / ANIMATION_STEPS;
        int dy = (next.getY() - current.getY()) / ANIMATION_STEPS;

        p.setCurrentX(p.getCurrentX() + dx);
        p.setCurrentY(p.getCurrentY() + dy);
        p.setStep(p.getStep() + 1);

        // 2. Verificar se o pacote chegou ao próximo salto
        if (p.getStep() >= ANIMATION_STEPS) {
          p.setCurrentDevice(next);
          p.setNextHop(null);
          p.setStep(0);
          p.setCurrentX(next.getX());
          p.setCurrentY(next.getY());

          // O novo dispositivo processa o pacote
          p.getCurrentDevice().processPacket(p);
        }
      } else if (p.getCurrentDevice() != null && p.getStep() == 0) {
        // Pacote parado após processamento (sem próximo salto)
        p.endSimulation();
        System.out.println("Pacote [ID:" + p.getId() + "] Descartado/Concluído.");
      }
    }

    // 3. Remoção Manual de Pacotes Concluídos/Descartados
    int newPacketCount = 0;
    for (int i = 0; i < packetCount; i++) {
      Packet p = packetsInTransit[i];
      if (p != null && p.isInTransit()) {
        packetsInTransit[newPacketCount] = p;
        newPacketCount++;
      }
    }
    for (int i = newPacketCount; i < packetCount; i++) {
      packetsInTransit[i] = null;
    }
    packetCount = newPacketCount;
  }

  // --- Getters para a GUI ---

  public Device[] getDevices() {
    return devices;
  }

  public int getDeviceCount() {
    return deviceCount;
  }

  public Packet[] getPacketsInTransit() {
    return packetsInTransit;
  }

  public int getPacketCount() {
    return packetCount;
  }
}