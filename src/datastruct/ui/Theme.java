package datastruct.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class Theme {
    public static final Color INK = new Color(43, 36, 64);
    public static final Color MUTED = new Color(121, 115, 139);
    public static final Color ACCENT = new Color(125, 75, 197);
    public static final Color TINT = new Color(240, 230, 253);
    public static final Color BG = new Color(247, 246, 251);
    public static final Color LINE = new Color(231, 226, 239);
    public static final Color CORAL = new Color(255, 151, 127);
    public static final Color PINK = new Color(236, 101, 173);
    public static final Color BLUE = new Color(104, 190, 235);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private Theme() { }

    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        for (String key : new String[] {"Label", "Button", "TextField", "TextArea", "ComboBox", "Spinner", "TabbedPane", "MenuItem", "CheckBox"})
            UIManager.put(key + ".font", BODY);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
        UIManager.put("TabbedPane.focus", ACCENT);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", TINT);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("ComboBoxUI", FlatComboUI.class.getName());
        UIManager.put("ComboBox.border", new RoundBorder(LINE, 10, new Insets(4, 7, 4, 3)));
        UIManager.put("Spinner.border", new RoundBorder(LINE, 10, new Insets(4, 7, 4, 4)));
        UIManager.put("TextField.border", new RoundBorder(LINE, 10, new Insets(8, 10, 8, 10)));
        UIManager.put("TabbedPaneUI", FlatTabsUI.class.getName());
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", MUTED);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));
        UIManager.put("Slider.focus", BG);
        UIManager.put("Separator.foreground", LINE);
        UIManager.put("Separator.background", LINE);
        UIManager.put("ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI");
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumb", new Color(187, 203, 210));
        UIManager.put("ScrollBar.thumbShadow", new Color(187, 203, 210));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(187, 203, 210));
        UIManager.put("ScrollBar.thumbHighlight", new Color(187, 203, 210));
        UIManager.put("ScrollBar.track", BG);
    }

    public static JButton button(String text, boolean primary) { return new Button(text, primary); }
    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(BODY.deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(INK);
        return label;
    }
    public static JTextArea prose(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(BODY);
        area.setForeground(MUTED);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setBorder(null);
        return area;
    }
    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static class RoundBorder extends javax.swing.border.AbstractBorder {
        private final Color color; private final int radius; private final Insets insets;
        public RoundBorder(Color color, int radius, Insets insets) { this.color = color; this.radius = radius; this.insets = insets; }
        @Override public Insets getBorderInsets(Component c) { return (Insets)insets.clone(); }
        @Override public Insets getBorderInsets(Component c, Insets into) { into.set(insets.top, insets.left, insets.bottom, insets.right); return into; }
        @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c.hasFocus() ? ACCENT : color); g.drawRoundRect(x, y, w - 1, h - 1, radius, radius); g.dispose();
        }
    }
    public static class FlatComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent component) { return new FlatComboUI(); }
        @Override protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(MUTED); g.setStroke(new BasicStroke(1.5f)); int x = getWidth()/2, y = getHeight()/2;
                    g.drawLine(x-4,y-2,x,y+2); g.drawLine(x,y+2,x+4,y-2); g.dispose();
                }
            };
            button.setBorder(null); button.setOpaque(false); button.setPreferredSize(new Dimension(23, 24)); return button;
        }
        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus) { g.setColor(Color.WHITE); g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height); }
    }
    public static class FlatTabsUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent component) { return new FlatTabsUI(); }
        @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean selected) { }
        @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] r, int i, Rectangle icon, Rectangle text, boolean selected) { }
        @Override protected void paintContentBorder(Graphics g, int placement, int selectedIndex) { }
        @Override protected void paintTabBackground(Graphics graphics, int p, int i, int x, int y, int w, int h, boolean selected) {
            Graphics2D g = (Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? Color.WHITE : BG); g.fillRoundRect(x+2,y+2,w-4,h,12,12);
            if (selected) { g.setColor(ACCENT); g.fillRoundRect(x+14,y+h-4,w-28,3,3,3); } g.dispose();
        }
    }
    public static JPanel spectrum() {
        return new JPanel() {
            { setPreferredSize(new Dimension(0, 4)); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics.create();
                g.setPaint(new LinearGradientPaint(0, 0, Math.max(1,getWidth()), 0,
                        new float[]{0,.3f,.63f,1}, new Color[]{CORAL,PINK,new Color(174,113,228),BLUE}));
                g.fillRect(0,0,getWidth(),getHeight()); g.dispose();
            }
        };
    }
    public static Icon structureIcon(int index) {
        return new Icon() {
            public int getIconWidth() { return 22; } public int getIconHeight() { return 22; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g=(Graphics2D)graphics.create(); g.translate(x,y); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color[]{new Color(215,115,93),new Color(176,84,154),ACCENT,new Color(67,150,182),new Color(151,94,201),new Color(70,135,186)}[index]);
                g.setStroke(new BasicStroke(1.6f));
                if(index==0) for(int i=0;i<3;i++) g.drawRoundRect(1+i*7,6,5,10,2,2);
                else if(index==1 || index==3) { g.drawLine(3,11,19,11); for(int i=0;i<3;i++){g.setColor(Color.WHITE);g.fillOval(i*7,7,7,7);g.setColor(index==1?PINK:BLUE);g.drawOval(i*7,7,7,7);} }
                else if(index==2) for(int i=0;i<3;i++) g.drawRoundRect(4,2+i*7,14,5,2,2);
                else if(index==4) { g.drawLine(11,5,4,16);g.drawLine(11,5,18,16);g.fillOval(8,1,6,6);g.fillOval(1,15,6,6);g.fillOval(15,15,6,6); }
                else for(int i=0;i<3;i++)g.fillRoundRect(2+i*7,14-i*5,4,7+i*5,2,2);
                g.dispose();
            }
        };
    }

    private static final class Button extends JButton {
        private final boolean primary;
        Button(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(BOLD);
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(primary ? Color.WHITE : INK);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary ? ACCENT : getBackground();
            if (!isEnabled()) fill = primary ? new Color(221, 211, 238) : BG;
            else if (getModel().isPressed()) fill = primary ? ACCENT.darker() : TINT;
            else if (getModel().isRollover()) fill = primary ? new Color(143, 93, 212) : TINT;
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 13, 13);
            g.setColor(hasFocus() ? ACCENT : primary ? fill : LINE);
            g.setStroke(new BasicStroke(hasFocus() ? 2 : 1));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
