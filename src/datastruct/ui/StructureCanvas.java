package datastruct.ui;

import datastruct.core.Step;
import datastruct.core.StructureType;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;

/** Responsive scene: fit-to-view by default, explicit zoom with scroll bars. */
public final class StructureCanvas extends JPanel implements Scrollable {
    private StructureType type = StructureType.VECTOR;
    private Step step;
    private double zoom;
    private int selected = -1;
    private final List<Hit> hits = new ArrayList<>();
    private BiConsumer<Integer, Integer> onSelect = (index, value) -> { };
    private BiConsumer<Integer, Integer> onEdit = (index, value) -> { };
    private Consumer<Selection> onContext = selection -> { };
    private Runnable onReposition = () -> { };
    private final Map<StructureType, Map<Integer, Point2D.Double>> layouts = new EnumMap<>(StructureType.class);
    private Hit dragged;
    private Point2D grab;
    private Point press;
    private boolean moved;
    private AffineTransform transform = new AffineTransform();
    private record Hit(Rectangle2D bounds, int index, Integer value) { }
    public record Selection(int index, Integer value, int x, int y) { }
    private static final Color AMBER = new Color(184, 117, 22);
    private static final Color AMBER_BG = new Color(255, 243, 218);

    public StructureCanvas() {
        setBackground(new Color(254, 253, 255));
        setToolTipText("Arraste os blocos para organizar o desenho. Duplo clique edita; botão direito mostra ações.");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                if (popup(event)) return;
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                moved = false;
                Hit hit = hitAt(event.getPoint());
                if (hit != null) {
                    selected = hit.index(); onSelect.accept(hit.index(), hit.value());
                    if (type != StructureType.SORTING) {
                        dragged = hit; Point2D p = logical(event.getPoint());
                        grab = new Point2D.Double(p.getX() - hit.bounds().getX(), p.getY() - hit.bounds().getY());
                        press = event.getPoint(); moved = false;
                    }
                    repaint();
                }
            }
            @Override public void mouseDragged(MouseEvent event) {
                if (dragged == null || event.getPoint().distance(press) < 4 && !moved) return;
                moved = true;
                Point2D p = logical(event.getPoint()); Dimension size = scene();
                double x = clamp(p.getX() - grab.getX(), 8, size.width - dragged.bounds().getWidth() - 8);
                double y = clamp(p.getY() - grab.getY(), 30, size.height - dragged.bounds().getHeight() - 32);
                positions().put(key(dragged.index(), dragged.value()), new Point2D.Double(x / size.width, y / size.height));
                repaint();
            }
            @Override public void mouseReleased(MouseEvent event) {
                popup(event);
                if (dragged != null && moved) onReposition.run();
                dragged = null;
            }
            @Override public void mouseMoved(MouseEvent event) {
                setCursor(Cursor.getPredefinedCursor(hitAt(event.getPoint()) == null ? Cursor.DEFAULT_CURSOR
                        : type == StructureType.SORTING ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR));
            }
            @Override public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                Hit hit = hitAt(event.getPoint());
                if (hit != null) {
                    selected = hit.index();
                    onSelect.accept(hit.index(), hit.value());
                    if (event.getClickCount() == 2 && !moved) onEdit.accept(hit.index(), hit.value());
                    repaint();
                }
            }
            private boolean popup(MouseEvent event) {
                if (!event.isPopupTrigger()) return false;
                Hit hit = hitAt(event.getPoint());
                if (hit != null) {
                    selected = hit.index(); onSelect.accept(hit.index(), hit.value());
                    onContext.accept(new Selection(hit.index(), hit.value(), event.getX(), event.getY())); repaint();
                }
                return true;
            }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
    }
    public void onSelect(BiConsumer<Integer, Integer> listener) { onSelect = listener; }
    public void onEdit(BiConsumer<Integer, Integer> listener) { onEdit = listener; }
    public void onContext(Consumer<Selection> listener) { onContext = listener; }
    public void onReposition(Runnable listener) { onReposition = listener; }
    public void arrange() { positions().clear(); selected = -1; repaint(); }
    public boolean hasManualPositions() { return !positions().isEmpty(); }
    private Map<Integer, Point2D.Double> positions() { return layouts.computeIfAbsent(type, ignored -> new HashMap<>()); }
    private int key(int index, Integer value) { return type == StructureType.BST ? value : index; }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private Point placed(int index, Integer value, int x, int y, int width, int height) {
        Point2D p = positions().get(key(index, value));
        if (p == null) return new Point(x, y);
        Dimension size = scene();
        return new Point((int)clamp(p.getX() * size.width, 8, size.width - width - 8),
                (int)clamp(p.getY() * size.height, 30, size.height - height - 32));
    }
    private Point2D logical(Point point) {
        try { return transform.inverseTransform(point, null); }
        catch (java.awt.geom.NoninvertibleTransformException error) { return point; }
    }
    private Hit hitAt(Point point) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (transform.createTransformedShape(hit.bounds()).contains(point)) return hit;
        }
        return null;
    }
    public void setZoom(double value) { zoom = value; revalidate(); repaint(); }
    public double zoom() { return zoom; }
    public void show(StructureType type, Step step) {
        this.type = type;
        this.step = step;
        selected = -1;
        dragged = null;
        getAccessibleContext().setAccessibleDescription(type.title + ". " + step.explanation()
                + " Valores: " + step.values() + (type == StructureType.BST ? ". " + describeTree(step.tree()) : ""));
        revalidate(); repaint();
    }
    private static String describeTree(Step.TreeNode node) {
        return node == null ? "vazio" : node.value() + " (esquerda " + describeTree(node.left())
                + ", direita " + describeTree(node.right()) + ")";
    }
    private static int depth(Step.TreeNode node) {
        return node == null ? 0 : 1 + Math.max(depth(node.left()), depth(node.right()));
    }
    private Dimension viewport() {
        if (getParent() instanceof JViewport view && view.getExtentSize().width > 0) return view.getExtentSize();
        return getWidth() > 0 ? getSize() : new Dimension(820, 420);
    }
    private int columns() {
        int n = step == null ? 10 : type == StructureType.VECTOR ? step.capacity() : Math.max(1, step.values().size());
        Dimension view = viewport();
        int best = 1; double bestScale = 0;
        for (int columns = 1; columns <= Math.min(10,n); columns++) {
            double scale = Math.min(Math.max(40,view.width-36.0)/(columns*108+20),
                    Math.max(40,view.height-36.0)/(((n+columns-1)/columns)*125+22));
            if (scale > bestScale) { bestScale = scale; best = columns; }
        }
        return best;
    }
    private Dimension scene() {
        if (step == null || step.values().isEmpty() && type != StructureType.VECTOR) return new Dimension(430, 180);
        int count = step.values().size();
        return switch (type) {
            case VECTOR, LINKED_LIST, QUEUE -> {
                int n = type == StructureType.VECTOR ? step.capacity() : count;
                int cols = Math.min(columns(), n);
                yield new Dimension(cols * 108 + 20, ((n + cols - 1) / cols) * 125 + 22);
            }
            case STACK -> new Dimension(330, count * 54 + 80);
            case BST -> new Dimension(Math.max(150, count * 72 + 22), Math.max(1, depth(step.tree()) - 1) * 74 + 80);
            case SORTING -> new Dimension(Math.max(430, count * 64 + 35), 305);
        };
    }
    @Override public Dimension getPreferredSize() {
        Dimension size = scene();
        return zoom == 0 ? new Dimension(640, 380) : new Dimension((int)(size.width * zoom) + 40, (int)(size.height * zoom) + 40);
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(640, 380); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return 150; }
    @Override public boolean getScrollableTracksViewportWidth() { return zoom == 0 || getPreferredSize().width <= viewport().width; }
    @Override public boolean getScrollableTracksViewportHeight() { return zoom == 0 || getPreferredSize().height <= viewport().height; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(236, 230, 245));
        for (int x = 18; x < getWidth(); x += 22) for (int y = 18; y < getHeight(); y += 22) g.fillOval(x, y, 1, 1);
        hits.clear();
        if (step == null) { g.dispose(); return; }
        Dimension size = scene();
        double scale = zoom > 0 ? zoom : Math.max(.03, Math.min(1.15,
                Math.min((getWidth() - 36.0) / size.width, (getHeight() - 36.0) / size.height)));
        double tx = Math.max(18, (getWidth() - size.width * scale) / 2);
        double ty = Math.max(18, (getHeight() - size.height * scale) / 2);
        transform = new AffineTransform();
        transform.translate(tx, ty); transform.scale(scale, scale);
        g.transform(transform);
        if (step.values().isEmpty() && type != StructureType.VECTOR) {
            center(g, "Sua estrutura começa aqui", size.width / 2, 72, 22, Theme.INK, true);
            center(g, "Insira valores ou carregue um exemplo no painel ao lado.", size.width / 2, 107, 13, Theme.MUTED, false);
        } else switch (type) {
            case VECTOR, LINKED_LIST, QUEUE -> drawSequence(g);
            case STACK -> drawStack(g);
            case BST -> drawTree(g);
            case SORTING -> drawBars(g, size);
        }
        g.dispose();
    }
    private void drawSequence(Graphics2D g) {
        int n = type == StructureType.VECTOR ? step.capacity() : step.values().size();
        int cols = Math.min(columns(), n);
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            points.add(placed(i, i < step.values().size() ? step.values().get(i) : null,
                    12 + (i % cols) * 108, 30 + (i / cols) * 125, 82, 64));
        }
        if (type != StructureType.VECTOR) for (int i = 0; i < n - 1; i++) {
            Point from = points.get(i), to = points.get(i + 1);
            if (hasManualPositions() || (i + 1) % cols != 0) connect(g, from, 82, 64, to, 82, 64);
            else {
                g.setColor(Theme.ACCENT); g.setStroke(new BasicStroke(1.5f));
                g.drawLine(from.x + 41, from.y + 70, from.x + 41, from.y + 91);
                g.drawLine(from.x + 41, from.y + 91, to.x + 41, from.y + 91);
                arrow(g, to.x + 41, from.y + 91, to.x + 41, to.y - 29);
            }
        }
        for (int i = 0; i < n; i++) {
            int x = points.get(i).x, y = points.get(i).y;
            boolean occupied = i < step.values().size();
            String tag = type == StructureType.VECTOR ? "índice " + i : "nó " + i;
            if (i == 0 && type != StructureType.VECTOR) tag = type == StructureType.QUEUE ? "início · sai" : "cabeça";
            if (i == n - 1 && type != StructureType.VECTOR) tag = i == 0 ? "início / fim" : type == StructureType.QUEUE ? "fim · entra" : "último";
            center(g, tag, x + 41, y - 12, 11, Theme.MUTED, false);
            node(g, x, y, 82, 64, occupied ? String.valueOf(step.values().get(i)) : "livre", i, occupied, false);
            if (type == StructureType.LINKED_LIST && i == n - 1)
                center(g, "próximo = null", x + 41, y + 85, 11, Theme.MUTED, false);
            else if (type == StructureType.VECTOR)
                center(g, "[" + i + "]", x + 41, y + 85, 11, Theme.MUTED, false);
        }
    }
    private void drawStack(Graphics2D g) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < step.values().size(); i++) points.add(placed(i, step.values().get(i), 105, 38 + i * 54, 120, 44));
        if (hasManualPositions()) for (int i = 0; i < points.size() - 1; i++)
            connect(g, points.get(i), 120, 44, points.get(i + 1), 120, 44);
        for (int i = 0; i < step.values().size(); i++) {
            Point p = points.get(i);
            node(g, p.x, p.y, 120, 44, String.valueOf(step.values().get(i)), i, true, false);
            if (i == 0) {
                center(g, "TOPO · entra / sai", p.x + 60, p.y - 12, 11, Theme.ACCENT, true);
            }
        }
        Point last = points.get(points.size() - 1);
        center(g, "base", last.x + 60, last.y + 66, 11, Theme.MUTED, false);
    }
    private void drawTree(Graphics2D g) {
        Map<Step.TreeNode, Point> positions = new IdentityHashMap<>();
        assign(step.tree(), 0, new int[] {0}, positions);
        positions.replaceAll((node, p) -> {
            Point placed = placed(step.values().indexOf(node.value()), node.value(), p.x - 30, p.y - 30, 60, 60);
            return new Point(placed.x + 30, placed.y + 30);
        });
        for (var entry : positions.entrySet()) {
            Step.TreeNode parent = entry.getKey(); Point point = entry.getValue();
            for (Step.TreeNode child : new Step.TreeNode[] { parent.left(), parent.right() }) if (child != null) {
                Point to = positions.get(child);
                double angle = Math.atan2(to.y - point.y, to.x - point.x);
                arrow(g, point.x + (int)(32 * Math.cos(angle)), point.y + (int)(32 * Math.sin(angle)),
                        to.x - (int)(33 * Math.cos(angle)), to.y - (int)(33 * Math.sin(angle)));
                center(g, child == parent.left() ? "E" : "D", (point.x + to.x) / 2,
                        (point.y + to.y) / 2 - 4, 10, Theme.MUTED, false);
            }
        }
        for (var entry : positions.entrySet()) {
            Point p = entry.getValue(); int value = entry.getKey().value();
            int index = step.values().indexOf(value);
            node(g, p.x - 30, p.y - 30, 60, 60, String.valueOf(value), index, true, true);
        }
    }
    private void assign(Step.TreeNode node, int depth, int[] index, Map<Step.TreeNode, Point> positions) {
        if (node == null) return;
        assign(node.left(), depth + 1, index, positions);
        positions.put(node, new Point(41 + index[0]++ * 72, 39 + depth * 74));
        assign(node.right(), depth + 1, index, positions);
    }
    private void drawBars(Graphics2D g, Dimension size) {
        int maxAbs = Math.max(1, step.values().stream().mapToInt(Math::abs).max().orElse(1));
        boolean negative = step.values().stream().anyMatch(value -> value < 0);
        int baseline = negative ? 151 : 246;
        int maxHeight = negative ? 108 : 205;
        int start = (size.width - step.values().size() * 64 + 14) / 2;
        g.setColor(Theme.LINE); g.drawLine(10, baseline, size.width - 10, baseline);
        for (int i = 0; i < step.values().size(); i++) {
            int value = step.values().get(i), x = start + i * 64;
            int height = Math.max(3, (int)((double)Math.abs(value) / maxAbs * maxHeight));
            int y = value < 0 ? baseline : baseline - height;
            boolean active = i == step.focus(), second = i == step.secondary();
            g.setColor(active ? Theme.ACCENT : second ? AMBER : new Color(181, 153, 223));
            g.fillRoundRect(x, y, 48, height, 8, 8);
            if (selected == i) { g.setColor(Theme.INK); g.drawRoundRect(x - 2, y - 2, 52, height + 4, 8, 8); }
            center(g, String.valueOf(value), x + 24, value < 0 ? y + height + 18 : y - 10, 15, Theme.INK, true);
            center(g, "[" + i + "]", x + 24, 295, 11, Theme.MUTED, false);
            hits.add(new Hit(new Rectangle2D.Double(x - 4, Math.min(y - 22, baseline - 22), 56, height + 48), i, value));
        }
    }
    private void node(Graphics2D g, int x, int y, int width, int height, String value, int index, boolean occupied, boolean circle) {
        boolean focus = type == StructureType.BST ? occupied && step.values().get(index) == step.focus() : index == step.focus();
        boolean secondary = type != StructureType.BST && index == step.secondary();
        Shape shape = circle ? new java.awt.geom.Ellipse2D.Double(x, y, width, height)
                : new java.awt.geom.RoundRectangle2D.Double(x, y, width, height, 13, 13);
        g.setColor(focus ? Theme.TINT : secondary ? AMBER_BG : occupied ? Color.WHITE : Theme.BG); g.fill(shape);
        g.setColor(focus ? Theme.ACCENT : secondary ? AMBER : selected == index ? Theme.INK : Theme.LINE);
        g.setStroke(new BasicStroke(focus || secondary || selected == index ? 2.3f : 1.4f)); g.draw(shape);
        center(g, value, x + width / 2, y + height / 2 + 6, occupied ? 19 : 12, occupied ? Theme.INK : Theme.MUTED, occupied);
        hits.add(new Hit(shape.getBounds2D(), index, occupied ? step.values().get(index) : null));
    }
    private static void center(Graphics2D g, String text, int x, int y, int size, Color color, boolean bold) {
        g.setFont(Theme.BODY.deriveFont(bold ? Font.BOLD : Font.PLAIN, size)); g.setColor(color);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
    private static void arrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(Theme.ACCENT); g.setStroke(new BasicStroke(1.5f)); g.drawLine(x1, y1, x2, y2);
        double a = Math.atan2(y2 - y1, x2 - x1);
        for (double d : new double[] { -.5, .5 }) g.drawLine(x2, y2,
                (int)(x2 - 7 * Math.cos(a + d)), (int)(y2 - 7 * Math.sin(a + d)));
    }
    private static void connect(Graphics2D g, Point from, int w1, int h1, Point to, int w2, int h2) {
        double x1 = from.x + w1 / 2.0, y1 = from.y + h1 / 2.0;
        double x2 = to.x + w2 / 2.0, y2 = to.y + h2 / 2.0;
        double dx = x2 - x1, dy = y2 - y1;
        if (Math.abs(dx) + Math.abs(dy) < 1) return;
        double a = Math.min((w1 / 2.0 + 4) / Math.max(.001, Math.abs(dx)), (h1 / 2.0 + 4) / Math.max(.001, Math.abs(dy)));
        double b = Math.min((w2 / 2.0 + 5) / Math.max(.001, Math.abs(dx)), (h2 / 2.0 + 5) / Math.max(.001, Math.abs(dy)));
        if (a + b >= 1) return;
        arrow(g, (int)(x1 + dx * a), (int)(y1 + dy * a), (int)(x2 - dx * b), (int)(y2 - dy * b));
    }
    public BufferedImage exportImage() {
        BufferedImage image = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); paint(graphics); graphics.dispose(); return image;
    }
    public List<Rectangle2D> elementBounds() {
        return hits.stream().map(hit -> transform.createTransformedShape(hit.bounds()).getBounds2D()).toList();
    }
}
