package application;

import models.*;
import gui.SimulationPanel;

import javax.swing.*;
import java.awt.*;

public class MainApp {

  // ==========================================================
  // ESTADOS GLOBAIS DA INTERFACE (Acessível por SimulationPanel)
  // ==========================================================
  public static String currentMode = "NONE";
  public static Device firstDeviceToConnect = null;

  // NOVO: Armazenamento temporário dos dados ANTES do clique no mapa
  public static String tempDeviceName = null;
  public static String tempDeviceIp = null;
  public static String tempDeviceMask = null;

  // ==========================================================
  // MÉTODOS AUXILIARES DE CONFIGURAÇÃO DE REDE
  // ==========================================================

  public static Router.Route createRoute(String network, String mask, String nextHopIp, int outputIndex) {
    return new Router.Route(network, mask, nextHopIp, outputIndex);
  }

  public static void connectDevices(Device d1, String ip1, String mask1,
      Device d2, String ip2, String mask2) throws Exception {

    // 1. Cria as interfaces
    NetworkInterface intf1 = new NetworkInterface(ip1, mask1);
    NetworkInterface intf2 = new NetworkInterface(ip2, mask2);

    // 2. Configura a referência mútua (vizinho)
    intf1.setNeighbor(d2);
    intf2.setNeighbor(d1);

    // 3. Adiciona aos arrays de interfaces dos dispositivos
    d1.addInterface(intf1);
    d2.addInterface(intf2);

    System.out.println("Conectado: " + d1.getName() + " (" + ip1 + ") a " + d2.getName() + " (" + ip2 + ")");
  }

  // ==========================================================
  // PONTO DE ENTRADA PRINCIPAL
  // ==========================================================
  public static void main(String[] args) {

    NetworkMap map = new NetworkMap();

    // ----------------------------------------------------
    // ⭐ REMOVIDO: Toda a configuração inicial da rede pronta foi removida
    // O mapa começará vazio, permitindo a adição manual.
    // ----------------------------------------------------

    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Simulador de Tráfego de Pacotes (Java SE)");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      SimulationPanel panel = new SimulationPanel(map);
      frame.add(panel, BorderLayout.CENTER);

      JPanel deviceControlPanel = createDeviceControlPanel(panel);
      frame.add(deviceControlPanel, BorderLayout.WEST);

      JPanel transmissionControlPanel = createTransmissionControlPanel(map);
      frame.add(transmissionControlPanel, BorderLayout.SOUTH);

      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
  }

  // ==========================================================
  // GERENCIAMENTO DA INTERFACE GRÁFICA (PAINÉIS DE CONTROLE)
  // ==========================================================

  // Diálogo para configuração de NOVA interface (usado na conexão de
  // dispositivos)
  public static String[] showConnectionDialog(String deviceName) {
    JTextField ipField = new JTextField(15);
    JTextField maskField = new JTextField(15);

    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Configuração de Interface para: " + deviceName));
    panel.add(new JLabel("Endereço IP:"));
    panel.add(ipField);
    panel.add(new JLabel("Máscara de Sub-rede:"));
    panel.add(maskField);

    int result = JOptionPane.showConfirmDialog(null, panel,
        "Configurar Conexão", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
      return new String[] { ipField.getText(), maskField.getText() };
    }
    return null;
  }

  // Diálogo para configuração de NOVO DISPOSITIVO (Nome e 1ª Interface)
  public static String[] showNewDeviceDialog(String deviceType) {
    JTextField nameField = new JTextField(15);
    JTextField ipField = new JTextField(15);
    JTextField maskField = new JTextField(15);

    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Configuração do Novo " + deviceType));
    panel.add(new JLabel("Nome do Dispositivo (Ex: H1, R1):"));
    panel.add(nameField);
    panel.add(new JLabel("IP da 1ª Interface:"));
    panel.add(ipField);
    panel.add(new JLabel("Máscara de Sub-rede:"));
    panel.add(maskField);

    int result = JOptionPane.showConfirmDialog(null, panel,
        "Configurar Novo Dispositivo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
      return new String[] { nameField.getText().trim(), ipField.getText().trim(), maskField.getText().trim() };
    }
    return null;
  }

  // Painel para Adicionar/Conectar Dispositivos
  private static JPanel createDeviceControlPanel(SimulationPanel panel) {
    JPanel control = new JPanel();
    control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
    control.setBorder(BorderFactory.createTitledBorder("Gerenciar Dispositivos"));
    control.setPreferredSize(new Dimension(180, 600));

    JButton btnAddHost = new JButton("Adicionar Host (Configurar)");
    JButton btnAddRouter = new JButton("Adicionar Router (Configurar)");
    JButton btnConnect = new JButton("Conectar Dispositivos");

    // Ação Host
    btnAddHost.addActionListener(e -> {
      String[] config = showNewDeviceDialog("Host");
      if (config != null && !config[0].isBlank()) {
        tempDeviceName = config[0];
        tempDeviceIp = config[1];
        tempDeviceMask = config[2];
        currentMode = "ADD_HOST";
        firstDeviceToConnect = null;
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        System.out.println("Modo ATIVO: Clique no mapa para posicionar o Host " + tempDeviceName);
      } else if (config != null) {
        JOptionPane.showMessageDialog(panel, "Nome do dispositivo é obrigatório.", "Erro", JOptionPane.ERROR_MESSAGE);
      }
    });

    // Ação Router
    btnAddRouter.addActionListener(e -> {
      String[] config = showNewDeviceDialog("Router");
      if (config != null && !config[0].isBlank()) {
        tempDeviceName = config[0];
        tempDeviceIp = config[1];
        tempDeviceMask = config[2];
        currentMode = "ADD_ROUTER";
        firstDeviceToConnect = null;
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        System.out.println("Modo ATIVO: Clique no mapa para posicionar o Router " + tempDeviceName);
      } else if (config != null) {
        JOptionPane.showMessageDialog(panel, "Nome do dispositivo é obrigatório.", "Erro", JOptionPane.ERROR_MESSAGE);
      }
    });

    // Ação Conectar
    btnConnect.addActionListener(e -> {
      currentMode = "CONNECT";
      firstDeviceToConnect = null;
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      System.out.println("Modo CONECTAR ATIVO: Clique no primeiro dispositivo.");
    });

    control.add(btnAddHost);
    control.add(Box.createVerticalStrut(10));
    control.add(btnAddRouter);
    control.add(Box.createVerticalStrut(20));
    control.add(btnConnect);
    control.add(Box.createVerticalGlue());

    return control;
  }

  // Painel para iniciar a transmissão de pacotes
  private static JPanel createTransmissionControlPanel(NetworkMap map) {
    JPanel panel = new JPanel();
    panel.setLayout(new FlowLayout());

    // Note que estes campos podem precisar de valores iniciais vazios ou de exemplo
    // já que não haverá dispositivos iniciais.
    JTextField txtOrigem = new JTextField("192.168.1.10", 10);
    JTextField txtDestino = new JTextField("10.0.0.50", 10);
    JTextField txtQtd = new JTextField("5", 3);
    JButton btnEnviar = new JButton("Enviar Pacotes");

    btnEnviar.addActionListener(e -> {
      String origem = txtOrigem.getText();
      String destino = txtDestino.getText();

      try {
        int qtd = Integer.parseInt(txtQtd.getText());
        map.startTransmission(origem, destino, qtd);
      } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(panel, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
      }
    });

    panel.add(new JLabel("Origem IP:"));
    panel.add(txtOrigem);
    panel.add(new JLabel("Destino IP:"));
    panel.add(txtDestino);
    panel.add(new JLabel("Qtd:"));
    panel.add(txtQtd);
    panel.add(btnEnviar);

    return panel;
  }
}