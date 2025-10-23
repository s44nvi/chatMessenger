import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    // GUI Components
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;
    
    public Client() {
        initGUI();
    }
    
    private void initGUI() {
        setTitle("LAN Chat Messenger");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(63, 81, 181));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("LAN Chat Messenger");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        statusLabel = new JLabel("Not Connected");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(255, 200, 200));
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        messageField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(63, 81, 181));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setPreferredSize(new Dimension(100, 40));
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Add all components
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        // Window closing
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        // Show connection dialog on startup
        SwingUtilities.invokeLater(() -> showConnectionDialog());
    }
    
    private void showConnectionDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JTextField serverField = new JTextField("localhost");
        JTextField portField = new JTextField("5555");
        JTextField usernameField = new JTextField(System.getProperty("user.name"));
        
        panel.add(new JLabel("Server IP:"));
        panel.add(serverField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Connect to Server", JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                username = "Anonymous";
            }
            
            connectToServer(server, port);
        } else {
            System.exit(0);
        }
    }
    
    private void connectToServer(String server, int port) {
        try {
            chatArea.append("Connecting to " + server + ":" + port + "...\n");
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Handle username prompt
            String serverMessage = in.readLine();
            if ("ENTER_USERNAME".equals(serverMessage)) {
                out.println(username);
            }
            
            chatArea.append("Connected successfully!\n");
            chatArea.append("=================================\n\n");
            statusLabel.setText("Connected as " + username);
            statusLabel.setForeground(new Color(150, 255, 150));
            sendButton.setEnabled(true);
            messageField.setEnabled(true);
            messageField.requestFocus();
            
            // Start message listener
            new Thread(new MessageListener()).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server: " + e.getMessage(), 
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            chatArea.append("Connection failed: " + e.getMessage() + "\n");
            statusLabel.setText("Connection Failed");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        
        if (!message.isEmpty() && out != null) {
            out.println(message);
            messageField.setText("");
        }
    }
    
    private void disconnect() {
        try {
            if (out != null) {
                out.println("/quit");
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    class MessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("\n*** Disconnected from server ***\n");
                    statusLabel.setText("Disconnected");
                    statusLabel.setForeground(Color.RED);
                    sendButton.setEnabled(false);
                    messageField.setEnabled(false);
                });
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
