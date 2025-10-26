import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client extends JFrame {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private String currentRoom;
    
    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel currentRoomLabel;
    
    public Client() {
        showConnectionDialog();
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
        
        int result = JOptionPane.showConfirmDialog(null, panel, 
            "Connect to Server", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            username = usernameField.getText().trim();
            
            if (username.isEmpty()) username = "Anonymous";
            
            connectToServer(server, port);
        } else {
            System.exit(0);
        }
    }
    
    private void connectToServer(String server, int port) {
        try {
            socket = new Socket(server, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            Message connectMsg = new Message(Message.MessageType.CONNECT, username, "");
            out.writeObject(connectMsg);
            out.flush();
            
            initGUI();
            new Thread(new MessageListener()).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, 
                "Could not connect to server: " + e.getMessage(), 
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void initGUI() {
        setTitle("Chat Messenger - " + username);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);
        
        JPanel chatPanel = createChatPanel();
        add(chatPanel, BorderLayout.CENTER);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        setVisible(true);
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setBackground(new Color(43, 43, 43));
        sidebar.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(43, 43, 43));
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel roomsLabel = new JLabel("Rooms");
        roomsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roomsLabel.setForeground(Color.WHITE);
        
        JButton addButton = new JButton("+");
        addButton.setFont(new Font("Arial", Font.BOLD, 20));
        addButton.setBackground(new Color(63, 81, 181));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setPreferredSize(new Dimension(45, 35));
        addButton.addActionListener(e -> showRoomDialog());
        
        headerPanel.add(roomsLabel, BorderLayout.WEST);
        headerPanel.add(addButton, BorderLayout.EAST);
        sidebar.add(headerPanel, BorderLayout.NORTH);
        
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setFont(new Font("Arial", Font.PLAIN, 14));
        roomList.setBackground(new Color(60, 60, 60));
        roomList.setForeground(Color.WHITE);
        roomList.setSelectionBackground(new Color(63, 81, 181));
        roomList.setBorder(new EmptyBorder(5, 5, 5, 5));
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                joinSelectedRoom();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(null);
        sidebar.add(scrollPane, BorderLayout.CENTER);
        
        return sidebar;
    }
    
    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(63, 81, 181));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        currentRoomLabel = new JLabel("Select or create a room");
        currentRoomLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentRoomLabel.setForeground(Color.WHITE);
        headerPanel.add(currentRoomLabel, BorderLayout.WEST);
        
        chatPanel.add(headerPanel, BorderLayout.NORTH);
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatPanel.add(chatScroll, BorderLayout.CENTER);
    
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
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
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    private void showRoomDialog() {
        String[] options = {"Create Room", "Join Room", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, 
            "What would you like to do?", "Room Options",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[2]);
        
        if (choice == 0) createRoom();
        else if (choice == 1) joinRoom();
    }
    
    private void createRoom() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField roomIdField = new JTextField();
        JTextField roomNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        
        panel.add(new JLabel("Room ID:"));
        panel.add(roomIdField);
        panel.add(new JLabel("Room Name:"));
        panel.add(roomNameField);
        panel.add(new JLabel("Password (optional):"));
        panel.add(passwordField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Create Room", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String roomId = roomIdField.getText().trim();
            String roomName = roomNameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (!roomId.isEmpty() && !roomName.isEmpty()) {
                try {
                    Message msg = new Message(Message.MessageType.CREATE_ROOM, username, "");
                    msg.setRoomId(roomId);
                    msg.setRoomName(roomName);
                    msg.setPassword(password);
                    out.writeObject(msg);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void joinRoom() {
        String roomId = JOptionPane.showInputDialog(this, 
            "Enter Room ID:", "Join Room", JOptionPane.PLAIN_MESSAGE);
        
        if (roomId != null && !roomId.trim().isEmpty()) {
            attemptJoinRoom(roomId.trim());
        }
    }
    
    private void joinSelectedRoom() {
        String selected = roomList.getSelectedValue();
        if (selected != null && !selected.trim().isEmpty()) {
            String roomId = selected.split("\\|")[0].trim();
            attemptJoinRoom(roomId);
        }
    }
    
    private void attemptJoinRoom(String roomId) {
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Enter room password:"), BorderLayout.NORTH);
        panel.add(passwordField, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Room Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());
            try {
                Message msg = new Message(Message.MessageType.JOIN_ROOM, username, "", roomId);
                msg.setPassword(password);
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && currentRoom != null) {
            try {
                Message msg = new Message(Message.MessageType.TEXT, username, text, currentRoom);
                out.writeObject(msg);
                out.flush();
                
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                chatArea.append(String.format("[%s] %s: %s\n", timestamp, username, text));
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                messageField.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (currentRoom == null) {
            JOptionPane.showMessageDialog(this, "Join a room first!");
        }
    }
    
    private void disconnect() {
        try {
            if (out != null) {
                Message msg = new Message(Message.MessageType.DISCONNECT, username, "");
                out.writeObject(msg);
                out.flush();
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    class MessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    Message message = (Message) in.readObject();
                    handleMessage(message);
                }
            } catch (Exception e) {
                System.out.println("Disconnected");
            }
        }
    }
    
    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case TEXT:
                    if (message.getRoomId() != null && message.getRoomId().equals(currentRoom)) {
                        if (!message.getSender().equals(username)) {
                            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                            chatArea.append(String.format("[%s] %s: %s\n", 
                                timestamp, message.getSender(), message.getContent()));
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        }
                    }
                    break;
                    
                case NOTIFICATION:
                    chatArea.append("*** " + message.getContent() + " ***\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    
                    if (message.getRoomId() != null && !message.getRoomId().isEmpty()) {
                        currentRoom = message.getRoomId();
                        String notif = message.getContent();
                        if (notif.contains("Joined room:")) {
                            String roomName = notif.substring(notif.indexOf(":") + 1).trim();
                            currentRoomLabel.setText("Room: " + roomName);
                        }
                    }
                    break;
                    
                case PASSWORD_INCORRECT:
                    JOptionPane.showMessageDialog(this, 
                        "Incorrect password! Try again.", 
                        "Access Denied", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case ROOM_LIST:
                    updateRoomList(message.getContent());
                    break;
            }
        });
    }
    
    private void updateRoomList(String roomData) {
        roomListModel.clear();
        if (roomData != null && !roomData.trim().isEmpty()) {
            String[] rooms = roomData.split(";");
            for (String room : rooms) {
                if (!room.trim().isEmpty()) {
                    String[] parts = room.split("\\|");
                    if (parts.length >= 3) {
                        String display = String.format("%s | %s (%s)", 
                            parts[0].trim(), parts[1].trim(), parts[2].trim() + " users");
                        roomListModel.addElement(display);
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client());
    }
}