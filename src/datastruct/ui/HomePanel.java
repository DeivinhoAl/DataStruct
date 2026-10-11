package datastruct.ui;

import datastruct.core.StructureType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class HomePanel extends JPanel {
    private JPanel hero;
    public HomePanel(Consumer<StructureType> open) {
        super(new BorderLayout(0, 22)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 28, 24, 28));
        BufferedImage loaded = null;
        try {
            var input = HomePanel.class.getResourceAsStream("/assets/cover.png");
            if (input != null) { try (input) { loaded = ImageIO.read(input); } }
            else loaded = ImageIO.read(Path.of("assets/cover.png").toFile());
        } catch (Exception ignored) { }
        final BufferedImage cover = loaded;
        hero = new JPanel() {
            { setPreferredSize(new Dimension(0, 305)); setMinimumSize(new Dimension(0, 200)); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setClip(new java.awt.geom.RoundRectangle2D.Double(0,0,getWidth(),getHeight(),24,24));
                g.setColor(Theme.TINT); g.fillRect(0,0,getWidth(),getHeight());
                if (cover != null) { double scale = Math.min((double)getWidth()/cover.getWidth(), (double)getHeight()/cover.getHeight());
                    int w = (int)(cover.getWidth()*scale), h = (int)(cover.getHeight()*scale);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(cover, getWidth()-w, (getHeight()-h)/2, w, h, null); }
                g.setColor(Theme.INK); g.setFont(Theme.BOLD.deriveFont(34f)); g.drawString("DataStruct",28,getHeight()/2);
                g.setColor(Theme.MUTED); g.setFont(Theme.BODY.deriveFont(14f)); g.drawString("Laboratório interativo",30,getHeight()/2+28);
                g.dispose();
            }
        };
        add(hero, BorderLayout.NORTH);
        JPanel bottom = Theme.panel(new BorderLayout(0, 14));
        JPanel heading = Theme.panel(new BorderLayout(0,4));
        heading.add(Theme.label("Explore as estruturas", 25, true), BorderLayout.NORTH);
        heading.add(Theme.label("Escolha uma estrutura para começar.", 14, false), BorderLayout.SOUTH);
        bottom.add(heading, BorderLayout.NORTH);
        JPanel cards = Theme.panel(new GridLayout(2,3,12,12));
        for (StructureType type : StructureType.values()) {
            JButton card = Theme.button("<html>" + type.title + "<br><span style='font-weight:normal;font-size:10px;color:#79738b'>" + type.subtitle + "</span></html>", false);
            card.setHorizontalAlignment(SwingConstants.LEFT); card.setIcon(Theme.structureIcon(type.ordinal())); card.setIconTextGap(13);
            card.addActionListener(event -> open.accept(type)); cards.add(card);
        }
        bottom.add(cards); add(bottom, BorderLayout.CENTER);
    }
    @Override public void doLayout() {
        if (hero != null) hero.setPreferredSize(new Dimension(0, Math.min(305,Math.max(150,getHeight()-310))));
        super.doLayout();
    }
}
