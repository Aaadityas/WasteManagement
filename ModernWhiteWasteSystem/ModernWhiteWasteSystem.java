import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class ModernWhiteWasteSystem extends JFrame {
    private List<WasteBin> wasteBins;
    private List<WasteBin> filteredBins;
    private List<CollectionRoute> collectionHistory;
    private JTable binTable;
    private DefaultTableModel tableModel;
    private JPanel dashboardPanel;
    private JPanel statsPanel;
    private Timer simulationTimer;
    private boolean darkMode = false;
    private final String DATA_FILE = "bins.csv";
    private final String HISTORY_FILE = "collection_history.csv";
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JLabel totalBinsLabel, criticalBinsLabel, avgFillLabel, co2SavedLabel;
    private int totalCollections = 0;
    private double co2Saved = 0;

    // Modern color palette
    private Color primaryColor = new Color(99, 102, 241);
    private Color successColor = new Color(16, 185, 129);
    private Color warningColor = new Color(245, 158, 11);
    private Color dangerColor = new Color(239, 68, 68);
    private Color bgLight = new Color(249, 250, 251);
    private Color bgDark = new Color(17, 24, 39);
    private Color cardLight = Color.WHITE;
    private Color cardDark = new Color(31, 41, 55);

    public ModernWhiteWasteSystem() {
        wasteBins = new ArrayList<>();
        filteredBins = new ArrayList<>();
        collectionHistory = new ArrayList<>();
        loadBins();
        loadHistory();
        setupUI();
        startSimulation();
    }

    private void loadBins() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        WasteBin bin = new WasteBin(parts[0], parts[1],
                                Integer.parseInt(parts[2]), parts[3],
                                Integer.parseInt(parts[4]));
                        wasteBins.add(bin);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            wasteBins.add(new WasteBin("BIN-001", "Main Gate", 100, "General"));
            wasteBins.add(new WasteBin("BIN-002", "Cafeteria", 80, "Organic"));
            wasteBins.add(new WasteBin("BIN-003", "Office Block A", 100, "Recyclable"));
            wasteBins.add(new WasteBin("BIN-004", "Parking Lot", 120, "General"));
            wasteBins.add(new WasteBin("BIN-005", "Garden Area", 80, "Organic"));
            wasteBins.add(new WasteBin("BIN-006", "Reception", 60, "Recyclable"));
        }
        filteredBins.addAll(wasteBins);
    }

    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        collectionHistory.add(new CollectionRoute(parts[0], parts[1], Integer.parseInt(parts[2])));
                    }
                }
                totalCollections = collectionHistory.size();
                co2Saved = totalCollections * 2.5; // 2.5 kg CO2 saved per optimized collection
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void saveBins() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (WasteBin bin : wasteBins) {
                pw.println(String.join(",", bin.getId(), bin.getLocation(),
                        String.valueOf(bin.getCapacity()), bin.getType(),
                        String.valueOf(bin.getCurrentLevel())));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveHistory() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(HISTORY_FILE))) {
            for (CollectionRoute route : collectionHistory) {
                pw.println(String.join(",", route.timestamp, route.bins, String.valueOf(route.efficiency)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupUI() {
        setTitle("Smart Waste Management System");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Main container with padding
        JPanel mainContainer = new JPanel(new BorderLayout(15, 15));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainContainer.setBackground(bgLight);

        // Modern Header with gradient effect
        JPanel headerPanel = createModernHeader();

        // Stats Panel (Top)
        statsPanel = createStatsPanel();

        // Center Panel (Dashboard + Table)
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setOpaque(false);

        // Search and Filter Panel
        JPanel searchPanel = createSearchPanel();

        // Dashboard with cards
        dashboardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        dashboardPanel.setBackground(bgLight);
        JScrollPane dashboardScroll = new JScrollPane(dashboardPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dashboardScroll.setBorder(null);
        dashboardScroll.setPreferredSize(new Dimension(0, 280));
        styleScrollPane(dashboardScroll);

        // Modern Table
        createModernTable();
        JScrollPane tableScroll = new JScrollPane(binTable);
        tableScroll.setBorder(createStyledBorder("Waste Bin Status"));
        styleScrollPane(tableScroll);

        // Split center panel
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dashboardScroll, tableScroll);
        centerSplit.setDividerLocation(280);
        centerSplit.setOpaque(false);

        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(centerSplit, BorderLayout.CENTER);

        // Control Panel with modern buttons
        JPanel controlPanel = createControlPanel();

        // Add all panels
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(statsPanel, BorderLayout.PAGE_START);
        mainContainer.add(centerPanel, BorderLayout.CENTER);
        mainContainer.add(controlPanel, BorderLayout.SOUTH);

        add(mainContainer);
        setLocationRelativeTo(null);
        refreshData();
    }

    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, primaryColor, getWidth(), 0, primaryColor.brighter());
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 100));
        headerPanel.setOpaque(false);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        leftPanel.setOpaque(false);

        JLabel iconLabel = new JLabel("🗑️");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Smart Waste Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Real-time monitoring & optimization");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        leftPanel.add(iconLabel);
        leftPanel.add(textPanel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 30));
        rightPanel.setOpaque(false);

        JButton themeToggle = createIconButton("🌙", "Toggle Theme");
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));

        JButton notifBtn = createIconButton("🔔", "Notifications");
        notifBtn.addActionListener(e -> showAlerts());

        rightPanel.add(notifBtn);
        rightPanel.add(themeToggle);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 120));

        totalBinsLabel = new JLabel("0");
        criticalBinsLabel = new JLabel("0");
        avgFillLabel = new JLabel("0%");
        co2SavedLabel = new JLabel("0 kg");

        panel.add(createStatCard("Total Bins", totalBinsLabel, "📊", primaryColor));
        panel.add(createStatCard("Critical Bins", criticalBinsLabel, "⚠️", dangerColor));
        panel.add(createStatCard("Avg Fill Level", avgFillLabel, "📈", warningColor));
        panel.add(createStatCard("CO₂ Saved", co2SavedLabel, "🌱", successColor));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String icon, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(cardLight);
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(Color.GRAY);

        topPanel.add(titleLbl, BorderLayout.NORTH);
        topPanel.add(iconLabel, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setOpaque(false);

        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        filterCombo = new JComboBox<>(new String[]{"All Types", "General", "Organic", "Recyclable"});
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"All Status", "NORMAL", "WARNING", "CRITICAL"});
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterBins(); }
            public void removeUpdate(DocumentEvent e) { filterBins(); }
            public void insertUpdate(DocumentEvent e) { filterBins(); }
        });

        filterCombo.addActionListener(e -> filterBins());
        statusCombo.addActionListener(e -> filterBins());

        panel.add(new JLabel("🔍"));
        panel.add(searchField);
        panel.add(filterCombo);
        panel.add(statusCombo);

        return panel;
    }

    private void createModernTable() {
        String[] columns = {"Bin ID", "Location", "Type", "Capacity (L)", 
                "Current (%)", "Status", "Last Updated"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        binTable = new JTable(tableModel);
        binTable.setRowHeight(40);
        binTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        binTable.setGridColor(new Color(240, 240, 240));
        binTable.setShowVerticalLines(false);
        binTable.setIntercellSpacing(new Dimension(0, 5));

        binTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        binTable.getTableHeader().setBackground(cardLight);
        binTable.getTableHeader().setForeground(Color.DARK_GRAY);
        binTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(230, 230, 230)));
        binTable.getTableHeader().setPreferredSize(new Dimension(0, 45));

        binTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
        binTable.setSelectionBackground(new Color(99, 102, 241, 30));
        binTable.setSelectionForeground(Color.BLACK);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 15));
        panel.setOpaque(false);

        JButton refreshBtn = createModernButton("Refresh", "🔄", primaryColor);
        JButton alertBtn = createModernButton("Alerts", "⚠️", dangerColor);
        JButton reportBtn = createModernButton("Report", "📄", successColor);
        JButton addBinBtn = createModernButton("Add Bin", "➕", primaryColor);
        JButton routeBtn = createModernButton("Optimize Route", "🗺️", warningColor);
        JButton analyticsBtn = createModernButton("Analytics", "📊", new Color(139, 92, 246));
        JButton historyBtn = createModernButton("History", "📜", new Color(59, 130, 246));
        JButton resetBtn = createModernButton("Reset", "↺", dangerColor);

        refreshBtn.addActionListener(e -> refreshData());
        alertBtn.addActionListener(e -> showAlerts());
        reportBtn.addActionListener(e -> generateReport());
        addBinBtn.addActionListener(e -> addNewBin());
        routeBtn.addActionListener(e -> optimizeCollectionRoute());
        analyticsBtn.addActionListener(e -> showAnalytics());
        historyBtn.addActionListener(e -> showCollectionHistory());
        resetBtn.addActionListener(e -> resetAllBins());

        panel.add(refreshBtn);
        panel.add(alertBtn);
        panel.add(reportBtn);
        panel.add(addBinBtn);
        panel.add(routeBtn);
        panel.add(analyticsBtn);
        panel.add(historyBtn);
        panel.add(resetBtn);

        return panel;
    }

    private JButton createModernButton(String text, String icon, Color color) {
        JButton btn = new JButton(icon + " " + text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2d.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(color.brighter());
                } else {
                    g2d.setColor(color);
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 40));
        return btn;
    }

    private JButton createIconButton(String icon, String tooltip) {
        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, getModel().isRollover() ? 40 : 20));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(50, 50));
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void toggleTheme(JButton toggleBtn) {
        darkMode = !darkMode;
        Color bg = darkMode ? bgDark : bgLight;
        Color card = darkMode ? cardDark : cardLight;
        
        getContentPane().getComponent(0).setBackground(bg);
        dashboardPanel.setBackground(bg);
        binTable.setBackground(card);
        binTable.setForeground(darkMode ? Color.WHITE : Color.BLACK);
        binTable.getTableHeader().setBackground(card);
        binTable.getTableHeader().setForeground(darkMode ? Color.WHITE : Color.DARK_GRAY);
        
        toggleBtn.setText(darkMode ? "☀️" : "🌙");
        toggleBtn.setToolTipText(darkMode ? "Light Mode" : "Dark Mode");
        
        // Update stat cards
        Component[] comps = statsPanel.getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel) {
                c.setBackground(card);
            }
        }
        
        refreshData();
    }

    private void filterBins() {
        String search = searchField.getText().toLowerCase();
        filteredBins.clear();
        for (WasteBin b : wasteBins) {
            boolean matches = b.getId().toLowerCase().contains(search) ||
                            b.getLocation().toLowerCase().contains(search);
            if (matches) filteredBins.add(b);
        }
        refreshData();
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        for (WasteBin bin : filteredBins) {
            Object[] row = {bin.getId(), bin.getLocation(), bin.getType(),
                    bin.getCapacity(), bin.getCurrentLevel() + "%",
                    bin.getStatus(), bin.getLastUpdated()};
            tableModel.addRow(row);
        }
        updateDashboard();
        updateStats();
        saveBins();
    }

    private void updateStats() {
        int total = wasteBins.size();
        long critical = wasteBins.stream().filter(b -> b.getCurrentLevel() >= 80).count();
        double avg = wasteBins.stream().mapToInt(WasteBin::getCurrentLevel).average().orElse(0);

        totalBinsLabel.setText(String.valueOf(total));
        criticalBinsLabel.setText(String.valueOf(critical));
        avgFillLabel.setText(String.format("%.1f%%", avg));
        co2SavedLabel.setText(String.format("%.1f kg", co2Saved));
    }

    private void updateDashboard() {
        dashboardPanel.removeAll();
        for (WasteBin bin : filteredBins) {
            dashboardPanel.add(createEnhancedBinCard(bin));
        }
        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    private JPanel createEnhancedBinCard(WasteBin bin) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Shadow effect
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(2, 2, getWidth(), getHeight(), 20, 20);
            }
        };
        
        card.setPreferredSize(new Dimension(200, 220));
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(darkMode ? cardDark : cardLight);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top section
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        String emoji = bin.getType().equals("Organic") ? "🍃" :
                      bin.getType().equals("Recyclable") ? "♻️" : "🗑️";
        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel idLabel = new JLabel(bin.getId());
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        idLabel.setForeground(darkMode ? Color.WHITE : Color.BLACK);

        JLabel locLabel = new JLabel(bin.getLocation());
        locLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        locLabel.setForeground(Color.GRAY);

        infoPanel.add(idLabel);
        infoPanel.add(locLabel);

        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(iconLabel, BorderLayout.EAST);

        // Progress section
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setOpaque(false);

        JLabel percentLabel = new JLabel(bin.getCurrentLevel() + "%");
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Color levelColor = bin.getCurrentLevel() >= 80 ? dangerColor :
                          bin.getCurrentLevel() >= 60 ? warningColor : successColor;
        percentLabel.setForeground(levelColor);

        JProgressBar levelBar = new JProgressBar(0, 100);
        levelBar.setValue(bin.getCurrentLevel());
        levelBar.setStringPainted(false);
        levelBar.setPreferredSize(new Dimension(0, 12));
        levelBar.setForeground(levelColor);
        levelBar.setBackground(new Color(levelColor.getRed(), levelColor.getGreen(), 
                                        levelColor.getBlue(), 30));
        levelBar.setBorderPainted(false);

        JLabel statusLabel = new JLabel(bin.getStatus());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(levelColor);

        progressPanel.add(percentLabel);
        progressPanel.add(Box.createVerticalStrut(8));
        progressPanel.add(levelBar);
        progressPanel.add(Box.createVerticalStrut(8));
        progressPanel.add(statusLabel);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(progressPanel, BorderLayout.CENTER);

        return card;
    }

    private void optimizeCollectionRoute() {
        List<WasteBin> criticalBins = wasteBins.stream()
                .filter(b -> b.getCurrentLevel() >= 70)
                .sorted((a, b) -> Integer.compare(b.getCurrentLevel(), a.getCurrentLevel()))
                .toList();

        if (criticalBins.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No bins require collection at this time.\nAll bins are below 70% capacity.",
                "Route Optimization", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder route = new StringBuilder("<html><body style='width: 400px; font-family: Segoe UI;'>");
        route.append("<h2 style='color: #6366f1;'>🗺️ Optimized Collection Route</h2>");
        route.append("<p><b>Bins to collect: ").append(criticalBins.size()).append("</b></p>");
        route.append("<p style='color: #10b981;'>Estimated time: ")
             .append(criticalBins.size() * 15).append(" minutes</p>");
        route.append("<p style='color: #10b981;'>CO₂ saved: 2.5 kg</p>");
        route.append("<hr><ol>");

        for (int i = 0; i < criticalBins.size(); i++) {
            WasteBin bin = criticalBins.get(i);
            route.append("<li><b>").append(bin.getId()).append("</b> - ")
                 .append(bin.getLocation())
                 .append(" <span style='color: #ef4444;'>(").append(bin.getCurrentLevel())
                 .append("%)</span></li>");
        }
        route.append("</ol></body></html>");

        int result = JOptionPane.showConfirmDialog(this, route.toString(), 
                "Collection Route", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            for (WasteBin bin : criticalBins) {
                bin.setCurrentLevel(0);
            }
            totalCollections++;
            co2Saved += 2.5;
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String binIds = criticalBins.stream().map(WasteBin::getId).reduce((a, b) -> a + ";" + b).orElse("");
            collectionHistory.add(new CollectionRoute(timestamp, binIds, criticalBins.size()));
            saveHistory();
            
            refreshData();
            JOptionPane.showMessageDialog(this, 
                "Collection completed successfully!\n" + criticalBins.size() + " bins emptied.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAnalytics() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> typeFill = new HashMap<>();

        for (WasteBin bin : wasteBins) {
            typeCount.merge(bin.getType(), 1, Integer::sum);
            typeFill.merge(bin.getType(), bin.getCurrentLevel(), Integer::sum);
        }

        panel.add(createLabelPair("📊 Total Collections:", totalCollections + " times"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabelPair("🌱 CO₂ Saved:", String.format("%.1f kg", co2Saved)));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabelPair("♻️ Waste Diverted:", String.format("%.1f kg", totalCollections * 45.0)));
        panel.add(Box.createVerticalStrut(15));

        JLabel typeHeader = new JLabel("Bin Type Analysis:");
        typeHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(typeHeader);
        panel.add(Box.createVerticalStrut(10));

        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            double avgFill = typeFill.get(type) / (double) count;
            panel.add(createLabelPair("  " + type + ":", 
                count + " bins (avg " + String.format("%.1f%%", avgFill) + ")"));
            panel.add(Box.createVerticalStrut(5));
        }

        panel.add(Box.createVerticalStrut(15));
        JLabel effHeader = new JLabel("Efficiency Metrics:");
        effHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(effHeader);
        panel.add(Box.createVerticalStrut(10));

        long overflowRisk = wasteBins.stream().filter(b -> b.getCurrentLevel() >= 90).count();
        panel.add(createLabelPair("  Overflow Risk:", overflowRisk + " bins"));
        panel.add(Box.createVerticalStrut(5));
        
        double efficiency = totalCollections > 0 ? (co2Saved / totalCollections) : 0;
        panel.add(createLabelPair("  Route Efficiency:", String.format("%.1f kg CO₂/route", efficiency)));

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Analytics Dashboard", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel createLabelPair(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 13));
        val.setForeground(primaryColor);
        p.add(lbl, BorderLayout.WEST);
        p.add(val, BorderLayout.EAST);
        return p;
    }

    private void showCollectionHistory() {
        if (collectionHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No collection history available yet.",
                "Collection History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] columns = {"Date & Time", "Bins Collected", "Efficiency"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (CollectionRoute route : collectionHistory) {
            model.addRow(new Object[]{route.timestamp, route.bins.split(";").length, 
                route.efficiency + " bins"});
        }

        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Collection History", JOptionPane.PLAIN_MESSAGE);
    }

    private void startSimulation() {
        simulationTimer = new Timer();
        simulationTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() {
                Random rand = new Random();
                for (WasteBin bin : wasteBins) {
                    if (bin.getCurrentLevel() < 100) {
                        int increase = bin.getType().equals("Organic") ? rand.nextInt(6) + 2 :
                                      bin.getType().equals("General") ? rand.nextInt(5) + 1 :
                                      rand.nextInt(3) + 1;
                        bin.setCurrentLevel(Math.min(100, bin.getCurrentLevel() + increase));
                    }
                }
                SwingUtilities.invokeLater(() -> refreshData());
            }
        }, 5000, 5000);
    }

    private void showAlerts() {
        List<WasteBin> criticalBins = wasteBins.stream()
                .filter(b -> b.getCurrentLevel() >= 80)
                .sorted((a, b) -> Integer.compare(b.getCurrentLevel(), a.getCurrentLevel()))
                .toList();

        StringBuilder alerts = new StringBuilder("<html><body style='width: 400px; font-family: Segoe UI;'>");
        alerts.append("<h2 style='color: #ef4444;'>⚠️ Active Alerts</h2>");

        if (criticalBins.isEmpty()) {
            alerts.append("<p style='color: #10b981; font-size: 14px;'>✓ All bins are operating normally.<br>No critical alerts at this time.</p>");
        } else {
            alerts.append("<p style='font-size: 13px;'><b>").append(criticalBins.size())
                  .append(" bin(s)</b> require immediate attention:</p><hr>");
            
            for (WasteBin bin : criticalBins) {
                String urgency = bin.getCurrentLevel() >= 95 ? "URGENT" :
                               bin.getCurrentLevel() >= 90 ? "HIGH" : "MEDIUM";
                String color = bin.getCurrentLevel() >= 95 ? "#dc2626" :
                              bin.getCurrentLevel() >= 90 ? "#ef4444" : "#f97316";
                
                alerts.append("<div style='margin: 10px 0; padding: 10px; background: #fef2f2; border-left: 4px solid ")
                      .append(color).append(";'>")
                      .append("<b style='color: ").append(color).append(";'>[").append(urgency).append("]</b> ")
                      .append("<b>").append(bin.getId()).append("</b><br>")
                      .append("<span style='color: #666;'>📍 ").append(bin.getLocation()).append("</span><br>")
                      .append("<span style='color: ").append(color).append("; font-size: 16px; font-weight: bold;'>")
                      .append(bin.getCurrentLevel()).append("%</span> capacity")
                      .append("</div>");
            }
        }
        alerts.append("</body></html>");

        JEditorPane editorPane = new JEditorPane("text/html", alerts.toString());
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "System Alerts", JOptionPane.WARNING_MESSAGE);
    }

    private void generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════╗\n");
        report.append("║        SMART WASTE MANAGEMENT - SYSTEM REPORT            ║\n");
        report.append("╚════════════════════════════════════════════════════════════╝\n\n");
        
        report.append("Generated: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("─────────────────────────────────────────────────────────────\n\n");

        report.append("📊 OVERVIEW\n");
        report.append("─────────────────────────────────────────────────────────────\n");
        int total = wasteBins.size();
        long critical = wasteBins.stream().filter(b -> b.getCurrentLevel() >= 80).count();
        long warning = wasteBins.stream().filter(b -> b.getCurrentLevel() >= 60 && b.getCurrentLevel() < 80).count();
        long normal = total - critical - warning;
        double avg = wasteBins.stream().mapToInt(WasteBin::getCurrentLevel).average().orElse(0);

        report.append(String.format("Total Bins:          %d\n", total));
        report.append(String.format("Normal Status:       %d (%.1f%%)\n", normal, (normal*100.0/total)));
        report.append(String.format("Warning Status:      %d (%.1f%%)\n", warning, (warning*100.0/total)));
        report.append(String.format("Critical Status:     %d (%.1f%%)\n", critical, (critical*100.0/total)));
        report.append(String.format("Average Fill Level:  %.1f%%\n\n", avg));

        report.append("🌱 ENVIRONMENTAL IMPACT\n");
        report.append("─────────────────────────────────────────────────────────────\n");
        report.append(String.format("Total Collections:   %d\n", totalCollections));
        report.append(String.format("CO₂ Saved:           %.1f kg\n", co2Saved));
        report.append(String.format("Waste Diverted:      %.1f kg\n\n", totalCollections * 45.0));

        report.append("📍 BIN DETAILS\n");
        report.append("─────────────────────────────────────────────────────────────\n");
        report.append(String.format("%-12s %-20s %-12s %6s %10s\n", 
                "BIN ID", "LOCATION", "TYPE", "LEVEL", "STATUS"));
        report.append("─────────────────────────────────────────────────────────────\n");

        for (WasteBin b : wasteBins) {
            report.append(String.format("%-12s %-20s %-12s %5d%% %10s\n",
                    b.getId(), b.getLocation(), b.getType(), 
                    b.getCurrentLevel(), b.getStatus()));
        }

        report.append("\n─────────────────────────────────────────────────────────────\n");
        report.append("End of Report\n");
        report.append("─────────────────────────────────────────────────────────────\n");

        JTextArea ta = new JTextArea(report.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(ta);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "System Report", JOptionPane.PLAIN_MESSAGE);
    }

    private void addNewBin() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField idField = new JTextField();
        JTextField locField = new JTextField();
        JTextField capField = new JTextField("100");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"General", "Organic", "Recyclable"});
        JSlider initialLevel = new JSlider(0, 100, 0);
        initialLevel.setMajorTickSpacing(25);
        initialLevel.setPaintTicks(true);
        initialLevel.setPaintLabels(true);

        panel.add(new JLabel("Bin ID:"));
        panel.add(idField);
        panel.add(new JLabel("Location:"));
        panel.add(locField);
        panel.add(new JLabel("Capacity (L):"));
        panel.add(capField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Initial Level:"));
        panel.add(initialLevel);

        int option = JOptionPane.showConfirmDialog(this, panel, "Add New Bin", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            try {
                String id = idField.getText().trim();
                String loc = locField.getText().trim();
                int cap = Integer.parseInt(capField.getText().trim());
                String type = (String) typeCombo.getSelectedItem();
                int level = initialLevel.getValue();

                if (id.isEmpty() || loc.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all required fields!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (wasteBins.stream().anyMatch(b -> b.getId().equals(id))) {
                    JOptionPane.showMessageDialog(this, "Bin ID already exists!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                WasteBin bin = new WasteBin(id, loc, cap, type, level);
                wasteBins.add(bin);
                filteredBins.add(bin);
                refreshData();
                JOptionPane.showMessageDialog(this, "Bin added successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid capacity value!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetAllBins() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all bins to 0%?\nThis action cannot be undone.",
                "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            for (WasteBin b : wasteBins) b.setCurrentLevel(0);
            refreshData();
            JOptionPane.showMessageDialog(this, "All bins have been reset to 0%!",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
    }

    private javax.swing.border.Border createStyledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                Color.DARK_GRAY
        );
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            ModernWhiteWasteSystem sys = new ModernWhiteWasteSystem();
            sys.setVisible(true);
        });
    }
}

// WasteBin Class
class WasteBin {
    private String id, location, type, lastUpdated;
    private int capacity, currentLevel;

    public WasteBin(String id, String location, int capacity, String type) {
        this(id, location, capacity, type, 0);
    }

    public WasteBin(String id, String location, int capacity, String type, int currentLevel) {
        this.id = id;
        this.location = location;
        this.capacity = capacity;
        this.type = type;
        this.currentLevel = currentLevel;
        updateTimestamp();
    }

    public void setCurrentLevel(int level) {
        this.currentLevel = level;
        updateTimestamp();
    }

    private void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getStatus() {
        if (currentLevel >= 80) return "CRITICAL";
        if (currentLevel >= 60) return "WARNING";
        return "NORMAL";
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public int getCurrentLevel() { return currentLevel; }
    public String getLastUpdated() { return lastUpdated; }
}

// CollectionRoute Class
class CollectionRoute {
    String timestamp;
    String bins;
    int efficiency;

    public CollectionRoute(String timestamp, String bins, int efficiency) {
        this.timestamp = timestamp;
        this.bins = bins;
        this.efficiency = efficiency;
    }
}

// Modern Table Cell Renderer
class ModernTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? table.getBackground() : new Color(249, 250, 251));
        }
        
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        if (column == 5) { // Status column
            String status = value.toString();
            if (status.equals("CRITICAL")) {
                setForeground(new Color(239, 68, 68));
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (status.equals("WARNING")) {
                setForeground(new Color(245, 158, 11));
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setForeground(new Color(16, 185, 129));
                setFont(getFont().deriveFont(Font.BOLD));
            }
        } else {
            setForeground(table.getForeground());
            setFont(table.getFont());
        }
        
        return c;
    }
}

// Modern Scroll Bar UI
class ModernScrollBarUI extends BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        thumbColor = new Color(200, 200, 200);
        thumbDarkShadowColor = new Color(180, 180, 180);
        thumbHighlightColor = new Color(220, 220, 220);
        thumbLightShadowColor = new Color(190, 190, 190);
        trackColor = new Color(245, 245, 245);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }

    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(thumbColor);
        g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
        g2.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(trackColor);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }
}

// Rounded Border
class RoundedBorder implements javax.swing.border.Border {
    private int radius;
    private Color color;

    RoundedBorder(int radius) {
        this(radius, null);
    }

    RoundedBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

    public boolean isBorderOpaque() {
        return false;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (color != null) {
            g2d.setColor(color);
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
}