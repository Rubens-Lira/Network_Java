package models;

import java.awt.Graphics;
import java.awt.Color;

public class Host extends Device {

  // Endereço do Gateway que este Host usará para sair da rede local
  private String gatewayAddress;

  public Host(int id, String name, int x, int y, String gateway) {
    super(id, name, x, y);
    this.gatewayAddress = gateway;
  }

  // ==========================================================
  // POLIMORFISMO: Lógica de Processamento de Pacotes (Roteamento Básico)
  // ==========================================================
  @Override
  public void processPacket(Packet packet) {
    // Assume que o Host tem apenas 1 interface relevante (getInterface(0))
    NetworkInterface primaryInterface = getInterface(0);

    if (primaryInterface == null) {
      System.out.println("Host " + getName() + " sem interface. Descartando pacote.");
      packet.endSimulation();
      return;
    }

    // 1. Verificar se é o Destino Final
    if (packet.getDestinationIp().equals(primaryInterface.getIpAddress())) {
      System.out.println("Host " + getName() + " recebeu o pacote. SUCESSO!");
      packet.endSimulation();
      return;
    }

    // 2. Lógica de Encaminhamento: Local vs. Gateway
    Device nextHopDevice = null;

    // **A:** Destino na Mesma Sub-rede (Envio Local)
    if (NetworkInterface.sameSubnet(
        primaryInterface.getIpAddress(),
        primaryInterface.getSubnetMask(),
        packet.getDestinationIp())) {
      // Em uma rede real, o Host enviaria um ARP. Aqui, assumimos que o destino
      // direto (vizinho) é quem tem o pacote ou o switch.
      nextHopDevice = primaryInterface.getNeighbor();
      System.out.println("Host " + getName() + " enviando para o vizinho (Sub-rede local).");

    }
    // **B:** Destino Fora da Sub-rede (Envio para o Gateway)
    else {
      // Se o destino é fora, o Host precisa enviar para o Gateway.
      // O vizinho primário (primaryInterface.getNeighbor()) DEVE ser o Gateway.
      nextHopDevice = primaryInterface.getNeighbor();

      if (nextHopDevice != null) {
        System.out.println("Host " + getName() + " enviando para o Gateway (" + gatewayAddress + ").");
      }
    }

    // 3. Execução do Salto
    if (nextHopDevice != null) {
      packet.setNextHop(nextHopDevice);
      packet.setCurrentDevice(this);
    } else {
      // Se o pacote não era para ele e não tem para onde ir (sem Gateway/Vizinho),
      // descarta
      System.out.println("Host " + getName() + " sem rota. Descartando pacote.");
      packet.endSimulation();
    }
  }

  // ==========================================================
  // POLIMORFISMO: Lógica de Desenho para a GUI
  // ==========================================================
  @Override
  public void draw(Graphics g) {
    // 1. Defina uma cor BEM chamativa para o teste
    g.setColor(Color.RED);
    // 2. Defina um tamanho GRANDE para o teste (por exemplo, 50x50 pixels)
    int radius = 25;

    // Desenha o corpo do host (círculo)
    g.fillOval(getX() - radius, getY() - radius, 2 * radius, 2 * radius);

    // Desenha o nome
    g.setColor(Color.BLACK);
    g.drawString(getName(), getX() + radius + 5, getY() + 5);
  }

  // Getters e Setters
  public String getGatewayAddress() {
    return gatewayAddress;
  }

  public void setGatewayAddress(String gatewayAddress) {
    this.gatewayAddress = gatewayAddress;
  }
}