package application;

import models.*;
import gui.SimulationPanel;

import javax.swing.*;
import java.awt.*;

public class MainApp {

    // ==========================================================
    // ESTADOS GLOBAIS DA INTERFACE (Acessível por SimulationPanel)
    // ==========================================================
    public static String currentMode = "NONE"; // ADD_HOST, ADD_ROUTER, CONNECT, SELECT_ROUTER_FOR_ROUTE
    public static Device firstDeviceToConnect = null;
    public static Router routerToEdit = null; // O roteador cuja tabela está sendo configurada

    // Armazenamento temporário dos dados ANTES do clique no mapa (para Host/Router)
    public static String tempDeviceName = null;
    public static String tempDeviceIp = null;
    public static String tempDeviceMask = null;

    // ==========================================================
    // MÉTODOS AUXILIARES DE CONFIGURAÇÃO DE REDE
    // ==========================================================

    /**
     * Conecta dois dispositivos, configurando interfaces e vizinhança mútua.
     */
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

        System.out.println("✅ Conectado: " + d1.getName() + " (" + ip1 + ") a " + d2.getName() + " (" + ip2 + ")");
    }

    // ==========================================================
    // PONTO DE ENTRADA PRINCIPAL
    // ==========================================================
    public static void main(String[] args) {
        
        // Acessa a ÚNICA instância do NetworkMap (Singleton)
        NetworkMap map = NetworkMap.getInstance(); 

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simulador de Tráfego de Pacotes (Java SE)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Painel principal de simulação/desenho
            SimulationPanel panel = new SimulationPanel(map);
            frame.add(panel, BorderLayout.CENTER);

            // Painel Lateral (Agrupa Controles de Dispositivos e Rotas)
            JPanel sideControls = new JPanel();
            sideControls.setLayout(new BoxLayout(sideControls, BoxLayout.Y_AXIS));
            sideControls.setPreferredSize(new Dimension(200, 600));

            // Adiciona Painéis de Controle
            JPanel deviceControlPanel = createDeviceControlPanel(panel);
            sideControls.add(deviceControlPanel);

            JPanel routeControlPanel = createRouteControlPanel(panel);
            sideControls.add(routeControlPanel);

            sideControls.add(Box.createVerticalGlue()); // Para empurrar tudo para cima

            frame.add(sideControls, BorderLayout.WEST);

            // Painel de Controle de Transmissão (Parte inferior)
            JPanel transmissionControlPanel = createTransmissionControlPanel(map);
            frame.add(transmissionControlPanel, BorderLayout.SOUTH);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
        
        });
    }

    // ==========================================================
    // GERENCIAMENTO DA INTERFACE GRÁFICA (Diálogos)
    // ==========================================================

    // Diálogo para configuração de NOVA interface (usado na conexão de dispositivos)
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
    
    // Diálogo para configuração de NOVA ROTA
    public static String[] showNewRouteDialog(String routerName, Router router) {
        JTextField netField = new JTextField("10.0.0.0", 15);
        JTextField maskField = new JTextField("255.255.255.0", 15);
        JTextField nextField = new JTextField("172.16.0.1", 15);
        
        // Use JComboBox para listar as interfaces existentes do roteador
        JComboBox<String> interfaceCombo = new JComboBox<>();
        for (int i = 0; i < router.getInterfaceCount(); i++) {
            interfaceCombo.addItem("Index " + i + " (" + router.getInterface(i).getIpAddress() + ")");
        }
        
        // Se o router não tiver interfaces, exibe uma mensagem
        if (router.getInterfaceCount() == 0) {
             interfaceCombo.addItem("Nenhuma interface cadastrada");
             interfaceCombo.setEnabled(false);
        }

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Configuração de Nova Rota para: " + routerName));
        panel.add(new JLabel("Rede de Destino (Ex: 10.0.0.0):"));
        panel.add(netField);
        panel.add(new JLabel("Máscara de Rede:"));
        panel.add(maskField);
        panel.add(new JLabel("Próximo Salto IP (Next Hop) ou 0.0.0.0 (Direto):"));
        panel.add(nextField);
        panel.add(new JLabel("Interface de Saída:"));
        panel.add(interfaceCombo);

        int result = JOptionPane.showConfirmDialog(null, panel,
            "Configurar Nova Rota", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int selectedIndex = interfaceCombo.getSelectedIndex();
            if (!interfaceCombo.isEnabled() || selectedIndex == -1) {
                JOptionPane.showMessageDialog(null, "O Roteador precisa de pelo menos uma interface.", "Erro", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            return new String[] { 
                netField.getText().trim(), 
                maskField.getText().trim(), 
                nextField.getText().trim(), 
                String.valueOf(selectedIndex) 
            };
        }
        return null;
    }


    // ==========================================================
    // GERENCIAMENTO DA INTERFACE GRÁFICA (Painéis de Controle)
    // ==========================================================

    // Painel para Adicionar/Conectar Dispositivos
    private static JPanel createDeviceControlPanel(SimulationPanel panel) {
        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createTitledBorder("Gerenciar Dispositivos"));
        control.setAlignmentX(Component.LEFT_ALIGNMENT); 

        JButton btnAddHost = new JButton("Adicionar Host (Configurar)");
        JButton btnAddRouter = new JButton("Adicionar Router (Configurar)");
        JButton btnConnect = new JButton("Conectar Dispositivos");

        // Configuração visual dos botões
        btnAddHost.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAddRouter.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAddHost.setMaximumSize(new Dimension(180, 30));
        btnAddRouter.setMaximumSize(new Dimension(180, 30));
        btnConnect.setMaximumSize(new Dimension(180, 30));


        // Ação Host
        btnAddHost.addActionListener(e -> {
            String[] config = showNewDeviceDialog("Host");
            if (config != null && !config[0].isBlank()) {
                tempDeviceName = config[0];
                tempDeviceIp = config[1];
                tempDeviceMask = config[2];
                currentMode = "ADD_HOST";
                firstDeviceToConnect = null;
                routerToEdit = null;
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
                routerToEdit = null;
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
            routerToEdit = null;
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            System.out.println("Modo CONECTAR ATIVO: Clique no primeiro dispositivo.");
        });

        control.add(btnAddHost);
        control.add(Box.createVerticalStrut(10));
        control.add(btnAddRouter);
        control.add(Box.createVerticalStrut(20));
        control.add(btnConnect);
        control.add(Box.createVerticalStrut(10));

        return control;
    }

    // Painel para configurar a Tabela de Roteamento
    private static JPanel createRouteControlPanel(SimulationPanel panel) {
        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createTitledBorder("Configurar Roteamento"));
        control.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnSelectRouter = new JButton("1. Selecionar Router");
        JButton btnAddRoute = new JButton("2. Adicionar Rota");
        JLabel lblSelectedRouter = new JLabel("Router Selecionado: N/A");

        // Garante que o JLabel tenha a mesma largura que os botões
        lblSelectedRouter.setMaximumSize(new Dimension(180, 30)); 
        lblSelectedRouter.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        btnSelectRouter.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAddRoute.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSelectRouter.setMaximumSize(new Dimension(180, 30));
        btnAddRoute.setMaximumSize(new Dimension(180, 30));

        // Ação 1: Selecionar Roteador no mapa
        btnSelectRouter.addActionListener(e -> {
            currentMode = "SELECT_ROUTER_FOR_ROUTE";
            firstDeviceToConnect = null;
            // routerToEdit será definido no MouseListener do SimulationPanel
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            System.out.println("Modo ATIVO: Clique no Router para editar a tabela.");
        });
        
        // Ação 2: Adicionar Rota
        btnAddRoute.addActionListener(e -> {
            if (routerToEdit == null) {
                JOptionPane.showMessageDialog(control, "Selecione um Router primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] routeConfig = showNewRouteDialog(routerToEdit.getName(), routerToEdit);
            if (routeConfig != null) {
                try {
                    String net = routeConfig[0];
                    String mask = routeConfig[1];
                    String next = routeConfig[2];
                    int outIndex = Integer.parseInt(routeConfig[3]); 
                    
                    RouteEntry newRoute = new RouteEntry(net, mask, next, outIndex);
                    routerToEdit.addRoute(newRoute); // Chama o método do Router.java
                    
                    System.out.println("✅ Rota adicionada ao " + routerToEdit.getName() + 
                                       ": Rede " + net + " via " + next + " (Interface " + outIndex + ")");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(control, "Índice de interface inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(control, "Erro ao criar rota: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Timer simples para atualizar o nome do router selecionado
        new Timer(200, e -> {
            if (routerToEdit != null) {
                lblSelectedRouter.setText("Router Selecionado: " + routerToEdit.getName());
            } else {
                lblSelectedRouter.setText("Router Selecionado: N/A");
            }
        }).start();


        control.add(lblSelectedRouter);
        control.add(Box.createVerticalStrut(10));
        control.add(btnSelectRouter);
        control.add(Box.createVerticalStrut(10));
        control.add(btnAddRoute);
        control.add(Box.createVerticalStrut(10));

        return control;
    }


    // Painel para iniciar a transmissão de pacotes
    private static JPanel createTransmissionControlPanel(NetworkMap map) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Iniciar Simulação"));

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
                System.out.println(">>> INÍCIO DA TRANSMISSÃO: " + qtd + " pacotes de " + origem + " para " + destino);
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