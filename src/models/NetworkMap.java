package models;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkMap {

    // 1. Variável estática e privada para armazenar a única instância
    private static NetworkMap instance;

    private static final int MAX_DEVICES = 100;
    private final Device[] devices;
    private int deviceCount = 0;
    private int nextDeviceId = 1000;

    private static final int MAX_PACKETS_IN_TRANSIT = 500;
    private final Packet[] packetsInTransit;
    private int packetCount = 0;
    
    // ... (sua constante ANIMATION_STEPS)
    private static final int ANIMATION_STEPS = 10;

    // 2. O CONSTRUTOR DEVE SER PRIVADO
    private NetworkMap() { // <--- MUITO IMPORTANTE: Mude de public para private
        this.devices = new Device[MAX_DEVICES];
        this.packetsInTransit = new Packet[MAX_PACKETS_IN_TRANSIT];
    }

    // 3. Método estático e público para obter a única instância (getInstance)
    public static NetworkMap getInstance() { // <--- MÉTODO QUE ESTAVA FALTANDO
        if (instance == null) {
            instance = new NetworkMap();
        }
        return instance;
    }
    // ============================
    // MÉTODOS DE DISPOSITIVOS
    // ============================

    // Adiciona dispositivo ao array
    public boolean addDevice(Device d) {
        if (deviceCount >= MAX_DEVICES) return false;
        devices[deviceCount++] = d;
        return true;
    }

    // Cria e adiciona Host ou Router
    public Device createAndAddDevice(String type, String name, String ip, String mask, int x, int y) throws Exception {
        Device newDevice;
        int id = nextDeviceId++;

        if ("Host".equals(type)) {
            String gateway = calculateGateway(ip, mask);
            newDevice = new Host(id, name, x, y, gateway);
        } else if ("Router".equals(type)) {
            newDevice = new Router(id, name, x, y);
        } else {
            throw new Exception("Tipo inválido: " + type);
        }

        // Cria primeira interface
        NetworkInterface intf = new NetworkInterface(ip, mask);
        newDevice.addInterface(intf);

        if (addDevice(newDevice)) {
            System.out.println("✅ Adicionado " + newDevice.getName() + " (" + x + "," + y + ")");
            return newDevice;
        } else {
            System.out.println("❌ Falha ao adicionar " + newDevice.getName());
            return null;
        }
    }

    // Remove dispositivo pelo ID
    public boolean removeDevice(int id) {
        for (int i = 0; i < deviceCount; i++) {
            if (devices[i].getId() == id) {
                // Desloca elementos para manter array compacto
                for (int j = i; j < deviceCount - 1; j++) {
                    devices[j] = devices[j + 1];
                }
                devices[--deviceCount] = null;
                return true;
            }
        }
        return false;
    }

    // Busca dispositivo por IP
    public Device findDeviceByIP(String ip) {
        for (int i = 0; i < deviceCount; i++) {
            Device d = devices[i];
            for (int j = 0; j < d.getInterfaceCount(); j++) {
                if (d.getInterface(j).getIpAddress().equals(ip)) return d;
            }
        }
        return null;
    }

    // Busca dispositivo por coordenadas
    public Device findDeviceByCoordinates(int x, int y, int tolerance) {
        for (int i = 0; i < deviceCount; i++) {
            Device d = devices[i];
            int dx = x - d.getX();
            int dy = y - d.getY();
            if (dx * dx + dy * dy <= tolerance * tolerance) return d;
        }
        return null;
    }

    // ============================
    // MÉTODOS DE PACOTES
    // ============================

    public void startTransmission(String sourceIp, String destinationIp, int quantity) {
        Device source = findDeviceByIP(sourceIp);
        if (source == null) {
            System.out.println("Erro: dispositivo fonte não encontrado para IP " + sourceIp);
            return;
        }

        for (int i = 0; i < quantity && packetCount < MAX_PACKETS_IN_TRANSIT; i++) {
            Packet p = new Packet(sourceIp, destinationIp, source);
            packetsInTransit[packetCount++] = p;
            source.processPacket(p);
        }
    }

    public void tickSimulation() {
        for (int i = 0; i < packetCount; i++) {
            Packet p = packetsInTransit[i];
            if (p == null || !p.isInTransit()) continue;

            Device next = p.getNextHop();
            Device current = p.getCurrentDevice();

            if (next != null) {
                int dx = (next.getX() - current.getX()) / ANIMATION_STEPS;
                int dy = (next.getY() - current.getY()) / ANIMATION_STEPS;

                p.setCurrentX(p.getCurrentX() + dx);
                p.setCurrentY(p.getCurrentY() + dy);
                p.setStep(p.getStep() + 1);

                if (p.getStep() >= ANIMATION_STEPS) {
                    p.setCurrentDevice(next);
                    p.setNextHop(null);
                    p.setStep(0);
                    p.setCurrentX(next.getX());
                    p.setCurrentY(next.getY());
                    p.getCurrentDevice().processPacket(p);
                }
            } else if (current != null && p.getStep() == 0) {
                p.endSimulation();
            }
        }

        // Remove pacotes concluídos
        int newCount = 0;
        for (int i = 0; i < packetCount; i++) {
            if (packetsInTransit[i] != null && packetsInTransit[i].isInTransit()) {
                packetsInTransit[newCount++] = packetsInTransit[i];
            }
        }
        for (int i = newCount; i < packetCount; i++) packetsInTransit[i] = null;
        packetCount = newCount;
    }

    // ============================
    // UTILITÁRIOS
    // ============================

    private String calculateGateway(String ip, String mask) {
        try {
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            byte[] maskBytes = InetAddress.getByName(mask).getAddress();
            byte[] netBytes = new byte[4];
            for (int i = 0; i < 4; i++) netBytes[i] = (byte) (ipBytes[i] & maskBytes[i]);
            netBytes[3] = 1; // último octeto = 1
            return InetAddress.getByAddress(netBytes).getHostAddress();
        } catch (UnknownHostException e) {
            return "0.0.0.0";
        }
    }

    // ============================
    // GETTERS
    // ============================

    public Device[] getDevices() { return devices; }
    public int getDeviceCount() { return deviceCount; }
    public Packet[] getPacketsInTransit() { return packetsInTransit; }
    public int getPacketCount() { return packetCount; }

    // ============================
    // MÉTODOS DE ITERAÇÃO (UTILITÁRIOS)
    // ============================

    // Itera sobre todos os dispositivos
    public void forEachDevice(DeviceConsumer consumer) {
        for (int i = 0; i < deviceCount; i++) {
            consumer.accept(devices[i]);
        }
    }

    // Itera sobre todos os pacotes
    public void forEachPacket(PacketConsumer consumer) {
        for (int i = 0; i < packetCount; i++) {
            consumer.accept(packetsInTransit[i]);
        }
    }

    // Interfaces funcionais para iteração
    public interface DeviceConsumer { void accept(Device d); }
    public interface PacketConsumer { void accept(Packet p); }
}
