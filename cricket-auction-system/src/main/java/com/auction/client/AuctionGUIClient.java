package com.auction.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class AuctionGUIClient extends JFrame {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 12345;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private JPanel connectionPanel;
    private JButton connectButton;
    private JLabel connectionStatus;
    
    private JPanel teamNamePanel;
    private JTextField teamNameField;
    private JButton submitTeamButton;
    
    private JPanel registrationPanel;
    private JTextField ownerNameField;
    private JTextField cityField;
    private JTextField budgetField;
    private JButton registerButton;
    
    private JPanel auctionPanel;
    private JTextArea messagesArea;
    private JScrollPane messagesScroll;
    private JLabel currentPlayerLabel;
    private JLabel currentBidLabel;
    private JLabel timerLabel;
    private JTextField bidAmountField;
    private JButton participateButton;
    private JButton placeBidButton;
    private JLabel budgetLabel;
    
    private JPanel teamPanel;
    private JList<String> playersList;
    private DefaultListModel<String> playersListModel;
    private JLabel teamInfoLabel;
    
    private boolean isRegistered = false;
    private boolean isParticipating = false;
    private Timer countdownTimer;
    private int timeRemaining = 30;
    private String currentTeamName;
    
    public AuctionGUIClient() {
        initializeGUI();
        setupEventHandlers();
    }
    
    private void initializeGUI() {
        setTitle("IPL Auction Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createConnectionPanel();
        createTeamNamePanel();
        createRegistrationPanel();
        createAuctionPanel();
        createTeamPanel();
        
        add(mainPanel);
        cardLayout.show(mainPanel, "CONNECTION");
    }
    
    private void createConnectionPanel() {
        connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connect to Auction Server"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("IPL Auction System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        connectionPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Server: " + SERVER_ADDRESS + ":" + PORT), gbc);
        
        connectButton = new JButton("Connect to Server");
        connectButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        connectionPanel.add(connectButton, gbc);
        
        connectionStatus = new JLabel("Click to connect");
        connectionStatus.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 3;
        connectionPanel.add(connectionStatus, gbc);
        
        mainPanel.add(connectionPanel, "CONNECTION");
    }
    
    private void createTeamNamePanel() {
        teamNamePanel = new JPanel(new GridBagLayout());
        teamNamePanel.setBorder(BorderFactory.createTitledBorder("Team Login"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel instructionLabel = new JLabel("Enter your team name:");
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        teamNamePanel.add(instructionLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        teamNamePanel.add(new JLabel("Team Name:"), gbc);
        
        teamNameField = new JTextField(20);
        gbc.gridx = 1;
        teamNamePanel.add(teamNameField, gbc);
        
        submitTeamButton = new JButton("Submit");
        submitTeamButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        teamNamePanel.add(submitTeamButton, gbc);
        
        mainPanel.add(teamNamePanel, "TEAM_NAME");
    }
    
    private void createRegistrationPanel() {
        registrationPanel = new JPanel(new GridBagLayout());
        registrationPanel.setBorder(BorderFactory.createTitledBorder("Team Registration - New Team"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        registrationPanel.add(new JLabel("Team Name:"), gbc);
        JLabel teamNameDisplay = new JLabel();
        teamNameDisplay.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 1;
        registrationPanel.add(teamNameDisplay, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        registrationPanel.add(new JLabel("Owner Name:"), gbc);
        ownerNameField = new JTextField(20);
        gbc.gridx = 1;
        registrationPanel.add(ownerNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        registrationPanel.add(new JLabel("City:"), gbc);
        cityField = new JTextField(20);
        gbc.gridx = 1;
        registrationPanel.add(cityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        registrationPanel.add(new JLabel("Budget:"), gbc);
        budgetField = new JTextField(20);
        gbc.gridx = 1;
        registrationPanel.add(budgetField, gbc);
        
        registerButton = new JButton("Register Team");
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        registrationPanel.add(registerButton, gbc);
        
        mainPanel.add(registrationPanel, "REGISTRATION");
    }
    
    private void createAuctionPanel() {
        auctionPanel = new JPanel(new BorderLayout());
        
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Current Auction"));
        
        currentPlayerLabel = new JLabel("Player: Waiting...");
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(currentPlayerLabel);
        
        currentBidLabel = new JLabel("Highest Bid: 0");
        currentBidLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(currentBidLabel);
        
        budgetLabel = new JLabel("Your Budget: 0");
        budgetLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(budgetLabel);
        
        timerLabel = new JLabel("Time: 30s");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timerLabel.setForeground(Color.RED);
        topPanel.add(timerLabel);
        
        auctionPanel.add(topPanel, BorderLayout.NORTH);
        
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messagesArea.setBackground(Color.BLACK);
        messagesArea.setForeground(Color.GREEN);
        messagesScroll = new JScrollPane(messagesArea);
        messagesScroll.setBorder(BorderFactory.createTitledBorder("Auction Messages"));
        messagesScroll.setPreferredSize(new Dimension(0, 300));
        auctionPanel.add(messagesScroll, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Bidding Controls"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        participateButton = new JButton("Participate in Auction");
        participateButton.setFont(new Font("Arial", Font.BOLD, 14));
        participateButton.setBackground(Color.GREEN);
        participateButton.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(participateButton, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        controlPanel.add(new JLabel("Bid Amount:"), gbc);
        
        bidAmountField = new JTextField(10);
        gbc.gridx = 1;
        controlPanel.add(bidAmountField, gbc);
        
        placeBidButton = new JButton("Place Bid");
        placeBidButton.setFont(new Font("Arial", Font.BOLD, 14));
        placeBidButton.setBackground(Color.BLUE);
        placeBidButton.setForeground(Color.WHITE);
        placeBidButton.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(placeBidButton, gbc);
        
        auctionPanel.add(controlPanel, BorderLayout.SOUTH);
        
        mainPanel.add(auctionPanel, "AUCTION");
    }
    
    private void createTeamPanel() {
        teamPanel = new JPanel(new BorderLayout());
        teamPanel.setBorder(BorderFactory.createTitledBorder("My Team"));
        teamPanel.setPreferredSize(new Dimension(300, 0));
        
        teamInfoLabel = new JLabel("Team: Not registered");
        teamInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        teamPanel.add(teamInfoLabel, BorderLayout.NORTH);
        
        playersListModel = new DefaultListModel<>();
        playersList = new JList<>(playersListModel);
        playersList.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane playersScroll = new JScrollPane(playersList);
        playersScroll.setBorder(BorderFactory.createTitledBorder("Acquired Players"));
        
        teamPanel.add(playersScroll, BorderLayout.CENTER);
        
        auctionPanel.add(teamPanel, BorderLayout.EAST);
    }
    
    private void setupEventHandlers() {
        connectButton.addActionListener(e -> connectToServer());
        submitTeamButton.addActionListener(e -> submitTeamName());
        registerButton.addActionListener(e -> registerTeam());
        participateButton.addActionListener(e -> participateInAuction());
        placeBidButton.addActionListener(e -> placeBid());
        
        bidAmountField.addActionListener(e -> placeBid());
        teamNameField.addActionListener(e -> submitTeamName());
        ownerNameField.addActionListener(e -> {
            if (registerButton.isEnabled()) registerTeam();
        });
    }
    
    private void connectToServer() {
        connectButton.setEnabled(false);
        connectionStatus.setText("Connecting...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    socket = new Socket(SERVER_ADDRESS, PORT);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        connectionStatus.setText("Connected successfully!");
                        startServerListener();
                        SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "TEAM_NAME"));
                    } else {
                        connectionStatus.setText("Failed to connect!");
                        connectButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    connectionStatus.setText("Connection error!");
                    connectButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
    
    private void submitTeamName() {
        String teamName = teamNameField.getText().trim();
        if (teamName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter team name!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        currentTeamName = teamName;
        submitTeamButton.setEnabled(false);
        out.println(teamName);
    }
    
    private void registerTeam() {
        String ownerName = ownerNameField.getText().trim();
        String city = cityField.getText().trim();
        String budget = budgetField.getText().trim();
        
        if (ownerName.isEmpty() || city.isEmpty() || budget.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Integer.parseInt(budget);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Budget must be a valid number!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        registerButton.setEnabled(false);
        
        Timer timer = new Timer();
        final int[] step = {0};
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    switch (step[0]) {
                        case 0:
                            out.println(ownerName);
                            break;
                        case 1:
                            out.println(city);
                            break;
                        case 2:
                            out.println(budget);
                            timer.cancel();
                            break;
                    }
                    step[0]++;
                });
            }
        }, 100, 500);
    }
    
    private void participateInAuction() {
        out.println("yes");
        participateButton.setEnabled(false);
        participateButton.setText("Participating...");
        participateButton.setBackground(Color.ORANGE);
        isParticipating = true;
    }
    
    private void placeBid() {
        String bidAmount = bidAmountField.getText().trim();
        if (bidAmount.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter bid amount!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int amount = Integer.parseInt(bidAmount);
            out.println("bid " + amount);
            bidAmountField.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void startServerListener() {
        Thread listener = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    final String message = response;
                    SwingUtilities.invokeLater(() -> processServerMessage(message));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Disconnected from server.");
                    JOptionPane.showMessageDialog(AuctionGUIClient.this, "Connection lost!", "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
        listener.setDaemon(true);
        listener.start();
    }
    
    private void updateTeamInfo(String teamName, String budget) {
        teamInfoLabel.setText("Team: " + teamName + " | Budget: " + budget);
    }
    
    private void updatePlayersList(String message) {
        if (message.contains("ACQUIRED:")) {
            try {
                String playerInfo = message.replace("ACQUIRED:", "").trim();
                playersListModel.addElement(playerInfo);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }
    
    private void processServerMessage(String message) {
        appendMessage(message);
        
        if (message.contains("Your budget:")) {
            try {
                String budget = message.substring(message.indexOf(":") + 1).trim();
                updateTeamInfo(currentTeamName, budget);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        updatePlayersList(message);
        
        if (message.contains("Enter your team name:")) {
            // Already handled
        } else if (message.contains("New team detected!")) {
            cardLayout.show(mainPanel, "REGISTRATION");
            ownerNameField.requestFocus();
        } else if (message.contains("Welcome back")) {
            isRegistered = true;
            JOptionPane.showMessageDialog(this, 
                "Welcome back! Entering auction portal...", 
                "Login Successful", 
                JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "AUCTION");
            
            try {
                currentTeamName = message.replace("Welcome back,", "").replace("!", "").trim();
            } catch (Exception e) {
                currentTeamName = "My Team";
            }
        } else if (message.contains("Registration complete!")) {
            isRegistered = true;
            JOptionPane.showMessageDialog(this, 
                "Registration complete! Entering auction...", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "AUCTION");
        } else if (message.contains("Registration full")) {
            JOptionPane.showMessageDialog(this, 
                "Sorry! Maximum number of teams reached. Try again later.", 
                "Server Full", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else if (message.contains("NEXT PLAYER:")) {
            handleNewPlayer(message);
        } else if (message.contains("Type 'yes' to participate")) {
            resetForNewAuction();
        } else if (message.contains("BIDDING STARTED!")) {
            handleBiddingStarted(message);
        } else if (message.contains("BID:")) {
            handleNewBid(message);
        } else if (message.contains("SOLD") || message.contains("UNSOLD")) {
            handleAuctionEnd();
        } else if (message.contains("budget")) {
            updateBudgetDisplay(message);
        } else if (message.contains("Auction completed")) {
            JOptionPane.showMessageDialog(this, 
                "Auction completed! Thank you for participating.", 
                "Auction Complete", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void handleNewPlayer(String message) {
        try {
            String[] parts = message.split(":")[1].split("\\(");
            String playerName = parts[0].trim();
            String role = parts[1].split("\\)")[0].trim();
            
            currentPlayerLabel.setText("Player: " + playerName + " (" + role + ")");
        } catch (Exception e) {
            currentPlayerLabel.setText("Player: " + message.substring(message.indexOf(":") + 1));
        }
    }
    
    private void resetForNewAuction() {
        participateButton.setEnabled(true);
        participateButton.setText("Participate in Auction");
        participateButton.setBackground(Color.GREEN);
        placeBidButton.setEnabled(false);
        isParticipating = false;
        stopTimer();
        timerLabel.setText("Time: --");
    }
    
    private void handleBiddingStarted(String message) {
        if (isParticipating) {
            placeBidButton.setEnabled(true);
            startBiddingTimer();
        }
        
        try {
            String currentBid = message.split(":")[1].trim();
            currentBidLabel.setText("Current Bid: " + currentBid);
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }
    
    private void handleNewBid(String message) {
        try {
            String[] parts = message.split(" bids ");
            String teamName = parts[0].replace("BID:", "").trim();
            String amount = parts[1].split(" for ")[0].trim();
            
            currentBidLabel.setText("Highest Bid: " + amount + " by " + teamName);
            startBiddingTimer();
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }
    
    private void handleAuctionEnd() {
        placeBidButton.setEnabled(false);
        stopTimer();
        timerLabel.setText("Auction Ended");
    }
    
    private void updateBudgetDisplay(String message) {
        if (message.contains("budget") && message.matches(".*\\d+.*")) {
            try {
                String budgetStr = message.replaceAll("[^0-9]", "");
                if (!budgetStr.isEmpty()) {
                    budgetLabel.setText("Your Budget: " + budgetStr);
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }
    
    private void startBiddingTimer() {
        stopTimer();
        timeRemaining = 30;
        
        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Time: " + timeRemaining + "s");
                    if (timeRemaining <= 10) {
                        timerLabel.setForeground(Color.RED);
                    } else {
                        timerLabel.setForeground(Color.BLUE);
                    }
                    
                    timeRemaining--;
                    if (timeRemaining < 0) {
                        stopTimer();
                        timerLabel.setText("Time Up!");
                    }
                });
            }
        }, 0, 1000);
    }
    
    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }
    
    private void appendMessage(String message) {
        messagesArea.append(message + "\n");
        messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AuctionGUIClient().setVisible(true);
        });
    }
}