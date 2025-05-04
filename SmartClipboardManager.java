import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Timer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SmartClipboardManager {
private String lastClipboardText = "";
private byte[] lastClipboardImageBytes = null;
private JFrame frame;
private DefaultListModel<DecryptedClipItem> model;
private JList<DecryptedClipItem> itemList;
private JTextArea textPreview;
private JLabel imagePreview;
private JTextField searchField;
private JPanel previewPanel;
private boolean darkTheme = false;
private Clipboard sysClipboard;
private Set<String> uniqueTracker = new HashSet<>();
private final String EXPORT_FILE = "clipboard_history.txt";
private static final String ALGORITHM = "AES";
private static final String KEY = "1234567890123456";


public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new SmartClipboardManager().createUI());
}

public void createUI() {
    sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    frame = new JFrame("Smart Clipboard Manager");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000, 600);
    frame.setLocationRelativeTo(null);

    model = new DefaultListModel<>();
    itemList = new JList<>(model);
    itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    itemList.setCellRenderer(new ClipRenderer());
    itemList.addListSelectionListener(e -> showPreview(itemList.getSelectedValue()));

    JScrollPane listScroll = new JScrollPane(itemList);

    previewPanel = new JPanel(new BorderLayout());
    previewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    textPreview = new JTextArea();
    textPreview.setLineWrap(true);
    textPreview.setWrapStyleWord(true);
    textPreview.setEditable(false);
    textPreview.setFont(new Font("Consolas", Font.PLAIN, 14));
    imagePreview = new JLabel("", SwingConstants.CENTER);
    imagePreview.setVerticalAlignment(SwingConstants.CENTER);

    searchField = new JTextField();
    searchField.addKeyListener(new KeyAdapter() {
        public void keyReleased(KeyEvent e) {
            filterList(searchField.getText());
        }
    });

    String[] predefinedTags = {"Text", "URL", "Email", "Image"};


DefaultListModel<String> tagListModel = new DefaultListModel<>();
for (String tag : predefinedTags) tagListModel.addElement(tag);

JList<String> tagList = new JList<>(tagListModel);
tagList.setDragEnabled(false);
tagList.setDropMode(DropMode.ON);
tagList.setTransferHandler(new TransferHandler() {
public boolean canImport(TransferHandler.TransferSupport support) {
return support.isDataFlavorSupported(DataFlavor.stringFlavor);
}


public boolean importData(TransferHandler.TransferSupport support) {
    try {
        if (!support.isDrop()) return false;
        JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
        String selectedTag = tagListModel.getElementAt(dl.getIndex());
        String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

        for (int i = 0; i < model.size(); i++) {
            DecryptedClipItem item = model.get(i);
            if (decrypt(item.content).equals(data)) {
                item.tag = selectedTag;
                itemList.repaint();
                break;
            }
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}


});
JScrollPane tagScrollPane = new JScrollPane(tagList);
tagScrollPane.setPreferredSize(new Dimension(120, 0));


    JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(new JLabel(" Search: "), BorderLayout.WEST);
    searchPanel.add(searchField, BorderLayout.CENTER);

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, previewPanel);
    splitPane.setDividerLocation(300);


    JButton pasteBtn = new JButton("📋 Paste");
    pasteBtn.addActionListener(e -> pasteToClipboard());

    JButton themeBtn = new JButton("🌙");
    themeBtn.addActionListener(e -> toggleTheme(themeBtn));

    JButton pinBtn = new JButton("📌 Pin");
    pinBtn.addActionListener(e -> togglePin());

    JButton deleteBtn = new JButton("❌ Delete");
    deleteBtn.addActionListener(e -> deleteSelected());

    JButton exportBtn = new JButton("💾 Export");
    exportBtn.addActionListener(e -> exportHistory());

    JButton importBtn = new JButton("📁 Import");
    importBtn.addActionListener(e -> importHistory());

    JButton notesBtn = new JButton("📝 Notes");
    notesBtn.addActionListener(e -> editNotes());


    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottomPanel.add(notesBtn);
    bottomPanel.add(pasteBtn);
    bottomPanel.add(pinBtn);
    bottomPanel.add(deleteBtn);
    bottomPanel.add(exportBtn);
    bottomPanel.add(importBtn);
    bottomPanel.add(themeBtn);

    frame.getContentPane().add(searchPanel, BorderLayout.NORTH);
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    monitorClipboard();
    applyTheme();
    frame.setVisible(true);
}

private void showPreview(DecryptedClipItem item) {
    previewPanel.removeAll();
    if (item != null) {
        if (item.imageBytes != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(item.imageBytes));
                imagePreview.setIcon(new ImageIcon(img));
                previewPanel.add(new JScrollPane(imagePreview), BorderLayout.CENTER);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            textPreview.setText("[Tag: " + item.tag + "]\n" +
                    decrypt(item.content) +
                    "\n\nNotes: " + item.notes +
                    "\nUsage: " + item.usageCount +
                    "\nCopied on: " + item.timestamp);
            previewPanel.add(new JScrollPane(textPreview), BorderLayout.CENTER);
        }
    }
    previewPanel.revalidate();
    previewPanel.repaint();
}


private void pasteToClipboard() {
    DecryptedClipItem item = itemList.getSelectedValue();
    if (item != null) {
        String decryptedContent = decrypt(item.content);
        if (item.imageBytes != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(item.imageBytes));
                sysClipboard.setContents(new TransferableImage(img), null);
                JOptionPane.showMessageDialog(frame, "🖼️ Image copied to clipboard!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            sysClipboard.setContents(new StringSelection(decryptedContent), null);
            item.usageCount++;
            itemList.repaint();
            JOptionPane.showMessageDialog(frame, "📋 Text copied to clipboard!\nUsage Count: " + item.usageCount);
        }
    }
}

private void togglePin() {
    DecryptedClipItem item = itemList.getSelectedValue();
    if (item != null) {
        item.pinned = !item.pinned;
        sortList();
    }
}

private void sortList() {
    List<DecryptedClipItem> sorted = Collections.list(model.elements());
    sorted.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
    model.clear();
    sorted.forEach(model::addElement);
}

private void deleteSelected() {
    DecryptedClipItem selected = itemList.getSelectedValue();
    if (selected != null) {
        model.removeElement(selected);
        uniqueTracker.remove(decrypt(selected.content));
    }
}

private void filterList(String query) {
    DefaultListModel<DecryptedClipItem> filtered = new DefaultListModel<>();
    for (int i = 0; i < model.size(); i++) {
        DecryptedClipItem item = model.get(i);
        if (decrypt(item.content).toLowerCase().contains(query.toLowerCase()) || item.tag.toLowerCase().contains(query.toLowerCase())) {
            filtered.addElement(item);
        }
    }
    itemList.setModel(filtered);
}

private void exportHistory() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(EXPORT_FILE))) {
        for (int i = 0; i < model.size(); i++) {
            DecryptedClipItem item = model.get(i);
            writer.write(decrypt(item.content).replaceAll("\n", "\\n"));
            writer.newLine();
        }
        JOptionPane.showMessageDialog(frame, "📤 Exported successfully!");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void importHistory() {
    try {
        List<String> lines = Files.readAllLines(Paths.get(EXPORT_FILE));
        for (String line : lines) {
            if (!uniqueTracker.contains(line)) {
                model.addElement(new DecryptedClipItem(encrypt(line), "imported"));
                uniqueTracker.add(line);
            }
        }
        JOptionPane.showMessageDialog(frame, "📥 Imported successfully!");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void applyTheme() {
    Color bg = darkTheme ? Color.DARK_GRAY : Color.WHITE;
    Color fg = darkTheme ? Color.WHITE : Color.BLACK;
    frame.getContentPane().setBackground(bg);
    previewPanel.setBackground(bg);
    itemList.setBackground(bg);
    itemList.setForeground(fg);
    textPreview.setBackground(bg);
    textPreview.setForeground(fg);
    imagePreview.setBackground(bg);
    imagePreview.setForeground(fg);
}

private void toggleTheme(JButton button) {
    darkTheme = !darkTheme;
    applyTheme();
    button.setText(darkTheme ? "☀️" : "🌙");
}

private void monitorClipboard() {
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
        public void run() {
            Transferable contents = sysClipboard.getContents(null);
            if (contents != null) {
                try {
                    // Handle text content
                    if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String data = (String) contents.getTransferData(DataFlavor.stringFlavor);

                        if (isSensitive(data)) {
                            System.out.println("⚠️ Skipped sensitive clipboard content");
                            return;
                        }

                        if (!data.equals(lastClipboardText)) {
                            lastClipboardText = data;

                            if (!uniqueTracker.contains(data)) {
                                String category = categorizeClipboardItem(data);
                                String encryptedContent = encrypt(smartCleanup(data));
                                DecryptedClipItem newItem = new DecryptedClipItem(encryptedContent, category, null);
                                newItem.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                model.addElement(newItem);
                                uniqueTracker.add(data);
                            }
                        }
                    }

                    // Handle image content
                    else if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                        BufferedImage bImg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = bImg.createGraphics();
                        g2d.drawImage(image, 0, 0, null);
                        g2d.dispose();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bImg, "png", baos);
                        byte[] imgBytes = baos.toByteArray();

                        if (!Arrays.equals(imgBytes, lastClipboardImageBytes)) {
                            lastClipboardImageBytes = imgBytes;
                            DecryptedClipItem imgItem = new DecryptedClipItem(encrypt("[Image]"), "image", imgBytes);
                            imgItem.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                            model.addElement(imgItem);
                        }
                    }

                    // Cleanup old entries
                    cleanupOldEntries();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }, 0, 1000); // Poll clipboard every 1 second

    // Move this out of the clipboard polling loop — setup once
    itemList.setDragEnabled(true);
    itemList.setTransferHandler(new TransferHandler("text") {
        protected Transferable createTransferable(JComponent c) {
            DecryptedClipItem selected = itemList.getSelectedValue();
            return selected != null ? new StringSelection(decrypt(selected.content)) : null;
        }

        public int getSourceActions(JComponent c) {
            return COPY;
        }
    });
}


private String smartCleanup(String data) {
    return data.replaceAll("[\\x00-\\x1F]", "");
}

private void cleanupOldEntries() {
    List<DecryptedClipItem> toRemove = new ArrayList<>();
    long now = System.currentTimeMillis();

    for (int i = 0; i < model.size(); i++) {
        DecryptedClipItem item = model.get(i);
        try {
            Date ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(item.timestamp);
            if ((now - ts.getTime()) > 7 * 24 * 60 * 60 * 1000L) {  // older than 7 days
                toRemove.add(item);
            }
        } catch (Exception ignored) {}
    }

    for (DecryptedClipItem oldItem : toRemove) {
        model.removeElement(oldItem);
        uniqueTracker.remove(decrypt(oldItem.content));
    }
}


private boolean isSensitive(String text) {
    String lower = text.toLowerCase();

    return lower.matches(".*(password\\s*=|pin\\s*=|otp|one\\s*time\\s*password).*") ||
           text.matches(".*\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b.*") ||  // card number
           text.matches(".*\\b\\d{6}\\b.*");  // possible OTP
}


private String categorizeClipboardItem(String data) {
    if (data.contains("http")) {
        return "URL";
    } else if (data.contains("@")) {
        return "Email";
    }
    return "Text";
}

private void editNotes() {
    DecryptedClipItem item = itemList.getSelectedValue();
    if (item != null) {
        String newNotes = JOptionPane.showInputDialog(frame, "Edit Notes:", item.notes != null ? item.notes : "");
        if (newNotes != null) {
            item.notes = newNotes;
            showPreview(item);
        }
    }
}


private String encrypt(String plainText) {
    try {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
        e.printStackTrace();
        return plainText;
    }
}

private static String decrypt(String encryptedText) {
    try {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        return new String(cipher.doFinal(decoded));
    } catch (Exception e) {
        return encryptedText;
    }
}

static class DecryptedClipItem {
    String content;
    String tag;
    byte[] imageBytes;
    boolean pinned = false;
    String timestamp;
    int usageCount = 0;
    String notes;

    public DecryptedClipItem(String content, String tag) {
        this.content = content;
        this.tag = tag;
    }

    public DecryptedClipItem(String content, String tag, byte[] imageBytes) {
        this(content, tag);
        this.imageBytes = imageBytes;
    }

    @Override
    public String toString() {
        return decrypt(content);
    }
}

    static class ClipRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    
            if (value instanceof DecryptedClipItem item) {
                String displayText = decrypt(item.content);
                if (displayText.length() > 80) {
                    displayText = displayText.substring(0, 80) + "...";
                }
    
                StringBuilder sb = new StringBuilder();
                if (item.pinned) {
                    sb.append("📌 ");
                }
    
                if ("image".equals(item.tag)) {
                    sb.append("[Image]");
                } else {
                    sb.append("[").append(item.tag).append("] ");
                    sb.append(displayText.replaceAll("\n", " "));
                }
    
                setText(sb.toString());
            }
    
            return this;
        }
    }


static class TransferableImage implements Transferable {
    private final Image image;

    public TransferableImage(Image image) {
        this.image = image;
    }

    public Object getTransferData(DataFlavor flavor) {
        return image;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }
}


}  