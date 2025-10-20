package models;

public class Packet {

  // ==========================================================
  // RASTREAMENTO DE ID ÚNICO
  // ==========================================================
  private int id;
  private static int nextId = 1;

  // ==========================================================
  // ATRIBUTOS DE ROTEAMENTO E ESTADO
  // ==========================================================
  private String sourceIp;
  private String destinationIp;
  private Device currentDevice;
  private Device nextHop;
  // NOVO: Adicione o dispositivo que enviou o pacote para evitar loops
  private Device previousDevice;
  private boolean inTransit;

  // ==========================================================
  // ATRIBUTOS DE ANIMAÇÃO
  // ==========================================================
  private int step;
  private int currentX;
  private int currentY;

  /**
   * Construtor para criar um novo pacote.
   */
  public Packet(String source, String destination, Device sourceDevice) {
    this.id = nextId++;

    this.sourceIp = source;
    this.destinationIp = destination;
    this.currentDevice = sourceDevice;

    // Inicializa como null, pois o dispositivo de origem não tem "anterior"
    this.previousDevice = null;

    this.nextHop = null;
    this.step = 0;
    this.inTransit = true;

    this.currentX = sourceDevice.getX();
    this.currentY = sourceDevice.getY();
  }

  public void endSimulation() {
    this.inTransit = false;
    System.out.println("Pacote ID " + this.id + " finalizou a simulação.");
  }

  // ==========================================================
  // GETTERS
  // ==========================================================
  public int getId() {
    return id;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public String getDestinationIp() {
    return destinationIp;
  }

  public Device getCurrentDevice() {
    return currentDevice;
  }

  // NOVO GETTER
  public Device getPreviousDevice() {
    return previousDevice;
  }

  public Device getNextHop() {
    return nextHop;
  }

  public int getStep() {
    return step;
  }

  public boolean isInTransit() {
    return inTransit;
  }

  public int getCurrentX() {
    return currentX;
  }

  public int getCurrentY() {
    return currentY;
  }

  // ==========================================================
  // SETTERS
  // ==========================================================
  public void setCurrentDevice(Device device) {
    this.currentDevice = device;
  }

  public void setNextHop(Device nextHop) {
    this.nextHop = nextHop;
  }

  public void setStep(int step) {
    this.step = step;
  }

  public void setCurrentX(int x) {
    this.currentX = x;
  }

  public void setCurrentY(int y) {
    this.currentY = y;
  }

  // NOVO SETTER
  public void setPreviousDevice(Device device) {
    this.previousDevice = device;
  }
}