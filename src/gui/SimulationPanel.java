package gui;

import models.Device;
import models.Packet;
import models.NetworkMap;
import application.MainApp;
import exceptions.InvalidIpException;
import exceptions.InvalidMaskException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

public class SimulationPanel extends JPanel implements ActionListener, MouseListener {

  private final NetworkMap networkMap;
  private final Timer timer;
  private final int TICK_RATE = 100;
  private final int CLICK_TOLERANCE = 15;

  public SimulationPanel(NetworkMap map) {
    this.networkMap = map;
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);

    addMouseListener(this);

    timer = new Timer(TICK_RATE, this);
    timer.start();
  }

  // ==========================================================
  // MÉTODO DE DESENHO (RENDERIZAÇÃO)
  // ==========================================================
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 1. Desenhar Conexões
    g2d.setColor(Color.LIGHT_GRAY);
    g2d.setStroke(new BasicStroke(2));
    for (int i = 0; i < networkMap.getDeviceCount(); i++) {
      Device d = networkMap.getDevices()[i];

      for (int j = 0; j < d.getInterfaceCount(); j++) {
        Device neighbor = d.getInterface(j).getNeighbor();
        if (neighbor != null && d.getId() < neighbor.getId()) {
          g2d.drawLine(d.getX(), d.getY(), neighbor.getX(), neighbor.getY());
        }
      }
    }
    g2d.setStroke(new BasicStroke(1));

    // 2. Desenhar Dispositivos
    for (int i = 0; i < networkMap.getDeviceCount(); i++) {
      Device d = networkMap.getDevices()[i];

      if (d == MainApp.firstDeviceToConnect) {
        g2d.setColor(new Color(255, 165, 0));
        g2d.drawOval(d.getX() - 15, d.getY() - 15, 30, 30);
      }
      d.draw(g2d);
    }

    // 3. Desenhar Pacotes em Trânsito
    g2d.setColor(Color.MAGENTA);
    for (int i = 0; i < networkMap.getPacketCount(); i++) {
      Packet p = networkMap.getPacketsInTransit()[i];
      g2d.fillOval(p.getCurrentX() - 4, p.getCurrentY() - 4, 8, 8);
    }
  }

  // ==========================================================
  // LOOP DE SIMULAÇÃO (CHAMADO PELO TIMER)
  // ==========================================================
  @Override
  public void actionPerformed(ActionEvent e) {
    networkMap.tickSimulation();
    repaint();
  }

  // ==========================================================
  // MOUSE LISTENER IMPLEMENTATION (Adição e Conexão de Dispositivos)
  // ==========================================================

  // Em SimulationPanel.java

  @Override
  public void mouseClicked(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();

    // A. Lógica de Adição de Dispositivo
    if (MainApp.currentMode.equals("ADD_HOST") || MainApp.currentMode.equals("ADD_ROUTER")) {
      String type = MainApp.currentMode.substring(4);

      try {
        // Tenta criar o dispositivo usando os dados temporários do MainApp
        networkMap.createAndAddDevice(
            type,
            MainApp.tempDeviceName,
            MainApp.tempDeviceIp,
            MainApp.tempDeviceMask,
            x,
            y);
      } catch (InvalidIpException | InvalidMaskException ex) {
        JOptionPane.showMessageDialog(this, "Erro de IP/Máscara: " + ex.getMessage() + ". Tente novamente.",
            "Erro de Configuração", JOptionPane.ERROR_MESSAGE);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Erro desconhecido ao adicionar dispositivo: " + ex.getMessage(), "Erro",
            JOptionPane.ERROR_MESSAGE);
      } finally {
        // Reseta o modo e os dados temporários após a tentativa
        MainApp.currentMode = "NONE";
        MainApp.tempDeviceName = null;
        MainApp.tempDeviceIp = null;
        MainApp.tempDeviceMask = null;
        setCursor(Cursor.getDefaultCursor());
        repaint();
        // *** REMOVIDO: return; *** (Era o motivo do erro de compilação)
      }
      return; // Retorna aqui para evitar processar o clique como CONNECT
    }

    // B. Lógica de Conexão de Dispositivos
    else if (MainApp.currentMode.equals("CONNECT")) {
      Device clickedDevice = networkMap.findDeviceByCoordinates(x, y, CLICK_TOLERANCE);

      if (clickedDevice == null) {
        System.out.println("Nenhum dispositivo encontrado na posição clicada. Modo CONNECT ativo.");
        return;
      }

      if (MainApp.firstDeviceToConnect == null) {
        // PRIMEIRO CLIQUE: Selecionar
        MainApp.firstDeviceToConnect = clickedDevice;
        System.out.println("Dispositivo 1 selecionado: " + clickedDevice.getName());
        repaint();
      } else if (MainApp.firstDeviceToConnect.equals(clickedDevice)) {
        // Clicou no mesmo dispositivo: Limpa a seleção
        MainApp.firstDeviceToConnect = null;
        System.out.println("Seleção desfeita.");
        repaint();
      } else {
        // SEGUNDO CLIQUE: Conectar
        Device d1 = MainApp.firstDeviceToConnect;
        Device d2 = clickedDevice;

        try {
          // Configurações via diálogo
          String[] config1 = MainApp.showConnectionDialog(d1.getName());
          if (config1 == null)
            throw new Exception("Configuração para " + d1.getName() + " cancelada.");
          String ip1 = config1[0];
          String mask1 = config1[1];

          String[] config2 = MainApp.showConnectionDialog(d2.getName());
          if (config2 == null)
            throw new Exception("Configuração para " + d2.getName() + " cancelada.");
          String ip2 = config2[0];
          String mask2 = config2[1];

          // Executar a conexão
          MainApp.connectDevices(d1, ip1, mask1, d2, ip2, mask2);
          System.out.println("Conexão estabelecida com sucesso!");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro de Conexão/Configuração",
              JOptionPane.ERROR_MESSAGE);
        } finally {
          // Limpar o modo e o estado de seleção
          MainApp.currentMode = "NONE";
          MainApp.firstDeviceToConnect = null;
          setCursor(Cursor.getDefaultCursor());
          repaint();
        }
      }
    }
  }

  // Métodos obrigatórios do MouseListener (deixados vazios)
  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }
}