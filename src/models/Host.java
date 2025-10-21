package models;

import java.awt.Graphics;
import java.awt.Color;

public class Host extends Device {

    // Endereço do Gateway que este Host usará para sair da rede local
    private String gatewayAddress;

    public Host(int id, String name, int x, int y, String gateway) {
        super(id, name, x, y, "resources/host_icon.png");
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
        String destinationIp = packet.getDestinationIp();

        // ** Acessa a instância global do mapa para encontrar dispositivos **
        NetworkMap map = NetworkMap.getInstance(); 

        // **A:** Destino na Mesma Sub-rede (Envio Local)
        if (NetworkInterface.sameSubnet(
                primaryInterface.getIpAddress(),
                primaryInterface.getSubnetMask(),
                destinationIp)) {
            
            // CORREÇÃO: Busca o dispositivo de destino DENTRO do NetworkMap
            nextHopDevice = map.findDeviceByIP(destinationIp);
            
            if (nextHopDevice != null) {
                System.out.println("Host " + getName() + " enviando para o destino local: " + nextHopDevice.getName() + " (Sub-rede local).");
            } else {
                System.out.println("Host " + getName() + ": Destino local não encontrado. Descartando pacote.");
            }
        }
        // **B:** Destino Fora da Sub-rede (Envio para o Gateway)
        else {
            // CORREÇÃO: Busca o dispositivo Gateway DENTRO do NetworkMap
            nextHopDevice = map.findDeviceByIP(this.gatewayAddress);

            if (nextHopDevice != null) {
                System.out.println("Host " + getName() + " enviando para o Gateway: " + nextHopDevice.getName() + " (" + gatewayAddress + ").");
            } else {
                System.out.println("Host " + getName() + ": Gateway indisponível/não encontrado. Descartando pacote.");
            }
        }

        // 3. Execução do Salto
        if (nextHopDevice != null) {
            // Seta o próximo nó de rede e o nó atual para a animação do NetworkMap
            packet.setNextHop(nextHopDevice);
            packet.setCurrentDevice(this); 
            // O NetworkMap.tickSimulation() fará o pacote "andar" até o nextHopDevice
        } else {
            // Se o pacote não era para ele e não tem para onde ir, descarta
            packet.endSimulation();
        }
    }

    // ==========================================================
    // POLIMORFISMO: Lógica de Desenho para a GUI
    // ==========================================================
    @Override
    public void draw(Graphics g) {
        int drawX = getX() - DEVICE_SIZE / 2;
        int drawY = getY() - DEVICE_SIZE / 2;

        if (deviceImage != null) {
            // Desenha a imagem centralizada no ponto (x, y)
            g.drawImage(deviceImage, drawX, drawY, null);
        } else {
            // Fallback: Se a imagem não carregar, desenha o quadrado amarelo original
            g.setColor(Color.RED);
            g.fillRect(drawX, drawY, DEVICE_SIZE, DEVICE_SIZE);
        }

        // Desenha o nome
        g.setColor(Color.BLACK);
        g.drawString(getName() + " (R-" + getId() + ")", getX() + DEVICE_SIZE / 2 + 5, getY() + 5);
    }

    // Getters e Setters
    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }
}