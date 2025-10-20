package models;

import java.awt.Graphics;
import java.awt.Color;

public class Router extends Device {
  // Tabela de roteamento simples: armazena qual interface usar para alcançar uma
  // rede
  // private final RoutingTable routingTable; // Supondo que você terá esta classe

  public Router(int id, String name, int x, int y) {
    super(id, name, x, y);
    // this.routingTable = new RoutingTable(); // Inicialização da tabela de
    // roteamento
  }

  // ==========================================================
  // POLIMORFISMO: Lógica de Processamento de Pacotes (Roteamento)
  // ==========================================================
  @Override
  public void processPacket(Packet packet) {
    System.out.println("Router " + getName() + " recebendo pacote. Encaminhando...");

    // Lógica de Roteamento Simples:
    // Por simplificação, vamos apenas tentar enviar para o próximo dispositivo
    // conectado em qualquer interface, se o destino não for local.

    // 1. Verificar se o pacote é para uma de suas interfaces (não deve acontecer
    // a menos que o pacote seja para um serviço rodando no router)
    for (int i = 0; i < getInterfaceCount(); i++) {
      if (packet.getDestinationIp().equals(getInterface(i).getIpAddress())) {
        System.out.println("Router " + getName() + " recebeu pacote para si mesmo. Descartando/Processando.");
        packet.endSimulation();
        return;
      }
    }

    // 2. Tentar encaminhar (Lógica simplificada sem tabela de roteamento completa)
    // Se houver uma tabela, você a usaria aqui: nextHop =
    // routingTable.lookup(destinationIp);
    Device nextHopDevice = null;

    // Simplesmente tenta o primeiro vizinho que não seja o Host de origem (se
    // conhecido)
    for (int i = 0; i < getInterfaceCount(); i++) {
      NetworkInterface intf = getInterface(i);
      Device neighbor = intf.getNeighbor();

      if (neighbor != null && !neighbor.equals(packet.getPreviousDevice())) {
        // Se o vizinho estiver na mesma sub-rede do destino (Lógica de roteamento
        // simples)
        if (NetworkInterface.sameSubnet(intf.getIpAddress(), intf.getSubnetMask(), packet.getDestinationIp())) {
          nextHopDevice = neighbor;
          System.out.println("Router " + getName() + " encaminhando para " + neighbor.getName() + " (Sub-rede).");
          break;
        }
      }
    }

    // 3. Execução do Salto
    if (nextHopDevice != null) {
      packet.setNextHop(nextHopDevice);
      packet.setCurrentDevice(this);
      // Salva o router atual para evitar loops simples
      packet.setPreviousDevice(this);
    } else {
      // Se o router não souber para onde enviar (drop)
      System.out.println("Router " + getName() + " sem rota para " + packet.getDestinationIp() + ". Descartando.");
      packet.endSimulation();
    }
  }

  // ==========================================================
  // POLIMORFISMO: Lógica de Desenho para a GUI
  // ==========================================================
  @Override
  public void draw(Graphics g) {
    // === ALTERAÇÃO TEMPORÁRIA PARA DEBUG VISUAL ===
    int halfSize = 25; // Aumentado para 25 (tamanho total 50x50)

    // Router desenhado como um quadrado AMARELO (mudado de vermelho)
    g.setColor(Color.YELLOW);
    g.fillRect(getX() - halfSize, getY() - halfSize, 2 * halfSize, 2 * halfSize);

    g.setColor(Color.BLACK);
    // Ajuste o X para que o texto comece após o novo quadrado maior
    g.drawString(getName() + " (R-" + getId() + ")", getX() + halfSize + 5, getY());

    // === FIM DA ALTERAÇÃO TEMPORÁRIA ===
  }
}