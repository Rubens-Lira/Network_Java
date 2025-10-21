package models;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon; 

public abstract class Device {

    private final int id;
    private String name;
    private final int x;
    private final int y;

    private static final int MAX_INTERFACES = 8;
    
    
    protected final int DEVICE_SIZE = 40; 
    protected Image deviceImage;

    private final NetworkInterface[] interfaces;
    private int interfaceCount = 0;


    public Device(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.interfaces = new NetworkInterface[MAX_INTERFACES];
    }


    protected Device(int id, String name, int x, int y, String imagePath) {
        this(id, name, x, y); 
        loadImage(imagePath);
    }


    private void loadImage(String imagePath) {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource(imagePath);
            if (imageUrl != null) {
                // Carrega a imagem e redimensiona para um tamanho fixo
                Image originalImage = new ImageIcon(imageUrl).getImage();
                this.deviceImage = originalImage.getScaledInstance(DEVICE_SIZE, DEVICE_SIZE, Image.SCALE_SMOOTH);
            } else {
                // Se a imagem não for encontrada, imprime um erro
                System.err.println("❌ Imagem não encontrada: " + imagePath + ". Usando fallback.");
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao carregar imagem '" + imagePath + "': " + e.getMessage());
            this.deviceImage = null;
        }
    }


    // Getters e setters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        if (name != null && !name.isBlank())
            this.name = name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getInterfaceCount() {
        return interfaceCount;
    }

    public NetworkInterface getInterface(int index) {
        if (index >= 0 && index < interfaceCount)
            return interfaces[index];
        return null;
    }


    public int getDeviceSize() {
        return DEVICE_SIZE;
    }

    public boolean addInterface(NetworkInterface netInterface) {
        if (interfaceCount >= MAX_INTERFACES)
            return false;

        for (int i = 0; i < interfaceCount; i++) {
            NetworkInterface ni = interfaces[i];
            if (ni.getIpAddress().equals(netInterface.getIpAddress()) ||
                ni.getNeighbor() == netInterface.getNeighbor())
                return false;
        }

        interfaces[interfaceCount] = netInterface;
        interfaceCount++;
        return true;
    }

    public NetworkInterface findInterfaceByNeighbor(Device neighbor) {
        for (int i = 0; i < interfaceCount; i++) {
            if (interfaces[i].getNeighbor() == neighbor)
                return interfaces[i];
        }
        return null;
    }

    // Métodos abstratos
    public abstract void processPacket(Packet packet);

    public abstract void draw(Graphics g);
}