package models;

import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Router extends Device {
    
    // Tabela de roteamento usando uma lista de RouteEntry (o array central de rotas)
    private final List<RouteEntry> routingTable; 

    // ==========================================================
    // CONSTRUTOR ATUALIZADO
    // Agora chama o construtor da superclasse com o caminho da imagem
    // ==========================================================
    public Router(int id, String name, int x, int y) {
        // O caminho da imagem deve ser relativo à pasta 'resources'
        super(id, name, x, y, "resources/router_icon.png"); 
        this.routingTable = new ArrayList<>(); // Inicializa a Tabela de Roteamento
    }
    
    // Método para adicionar entradas à tabela (usado na fase de configuração)
    public void addRoute(RouteEntry entry) {
        this.routingTable.add(entry);
    }
    
    /**
     * O coração do roteador: Busca a rota mais específica para o IP de destino.
     * Implementa o Longest Prefix Match.
     */
    private RouteEntry lookupRoute(String destinationIp) {
        RouteEntry bestMatch = null;
        int longestPrefix = -1; // Usado para encontrar a rota mais específica

        for (RouteEntry entry : this.routingTable) {
            
            // 1. Verifica se o IP de destino está na sub-rede da entrada de rota
            if (NetworkInterface.sameSubnet(
                entry.networkAddress, 
                entry.subnetMask, 
                destinationIp)) {
                
                // 2. Calcula o comprimento do prefixo para determinar a especificidade
                // (Presume que NetworkInterface.calculatePrefixLength existe e funciona)
                int prefixLength = NetworkInterface.calculatePrefixLength(entry.subnetMask);
                
                // 3. Verifica se esta rota é mais específica do que a melhor rota atual
                if (prefixLength > longestPrefix) {
                    longestPrefix = prefixLength;
                    bestMatch = entry;
                }
            }
        }
        return bestMatch;
    }

    // ==========================================================
    // POLIMORFISMO: Lógica de Processamento de Pacotes (SEM MUDANÇAS)
    // ==========================================================
    @Override
    public void processPacket(Packet packet) {
        // ... (Este bloco permanece o mesmo, pois a lógica de roteamento está correta) ...

        // 1. Verificar se é para o roteador (para fins de simulação, ignoramos)
        for (int i = 0; i < getInterfaceCount(); i++) {
            if (packet.getDestinationIp().equals(getInterface(i).getIpAddress())) {
                System.out.println("Router " + getName() + " recebeu pacote para si mesmo. Descartando/Processando.");
                packet.endSimulation();
                return;
            }
        }

        // 2. BUSCA NA TABELA DE ROTEAMENTO
        RouteEntry route = lookupRoute(packet.getDestinationIp());
        Device nextHopDevice = null;
        NetworkMap map = NetworkMap.getInstance(); // Acesso ao mapa

        if (route == null) {
            // Nenhuma rota encontrada, nem mesmo a rota padrão (0.0.0.0/0)
            System.out.println("Router " + getName() + " sem rota para " + packet.getDestinationIp() + ". Descartando.");
            packet.endSimulation();
            return;
        }

        // 3. Determinar o Próximo Salto (Next Hop)
        
        // Obtém a interface de saída determinada pela rota
        NetworkInterface outgoingInterface = getInterface(route.outputInterfaceIndex);
        
        if (outgoingInterface == null) {
            System.out.println("Router " + getName() + ": Erro, interface de saída inválida. Descartando.");
            packet.endSimulation();
            return;
        }

        // ** Caso 1: Rota Diretamente Conectada (Next Hop é o Destino Final) **
        if (route.nextHopIp.equals("0.0.0.0")) {
            // O próximo salto é o próprio dispositivo de destino final, conectado diretamente à interface.
            nextHopDevice = map.findDeviceByIP(packet.getDestinationIp());
        } 
        // ** Caso 2: Rota Remota (Next Hop é outro Router) **
        else {
            // O próximo salto é o roteador vizinho cujo IP está na tabela.
            nextHopDevice = map.findDeviceByIP(route.nextHopIp);
        }
        
        // 4. Execução do Salto
        if (nextHopDevice != null) {
            System.out.println("Router " + getName() + " encaminhando via " + outgoingInterface.getIpAddress() + " para " + nextHopDevice.getName());
            packet.setNextHop(nextHopDevice);
            packet.setCurrentDevice(this);
            // setPreviousDevice é importante para simular o "pulo"
            packet.setPreviousDevice(this); 
        } else {
            System.out.println("Router " + getName() + ": Próximo Salto (" + (route.nextHopIp.equals("0.0.0.0") ? "Destino Final" : route.nextHopIp) + ") não encontrado. Descartando.");
            packet.endSimulation();
        }
    }

    // ==========================================================
    // POLIMORFISMO: Lógica de Desenho para a GUI (ATUALIZADA)
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
            g.setColor(Color.YELLOW);
            g.fillRect(drawX, drawY, DEVICE_SIZE, DEVICE_SIZE);
        }

        // Desenha o nome
        g.setColor(Color.BLACK);
        g.drawString(getName() + " (R-" + getId() + ")", getX() + DEVICE_SIZE / 2 + 5, getY() + 5);
    }
}