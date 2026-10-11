package datastruct;

import datastruct.core.Action;
import datastruct.core.StructureType;
import datastruct.core.StudioSession;
import datastruct.ui.Theme;
import datastruct.ui.Workbench;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class WorkbenchTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static void layout(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) layout(child);
        }
    }
    private static Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component result = named(container, name); if (result != null) return result;
            }
        }
        return null;
    }
    private static BufferedImage render(Workbench workbench, int width, int height) {
        workbench.setSize(width, height);
        for (int i = 0; i < 5; i++) layout(workbench);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); workbench.printAll(graphics); graphics.dispose(); return image;
    }
    private static void editing() {
        StudioSession session = new StudioSession();
        for (String invalid : new String[] {",", ";;;", " ", "1, abc", "1000"}) {
            try { StudioSession.parseValues(invalid); throw new AssertionError("invalid input accepted: " + invalid); }
            catch (IllegalArgumentException expected) { check(true, "invalid input rejected"); }
        }
        session.execute(StructureType.VECTOR, Action.VECTOR_ADD, "12, 7; 24 5", null);
        check(session.simulator().values(StructureType.VECTOR).equals(List.of(12, 7, 24, 5)), "bulk separators");
        session.undo(); check(session.simulator().values(StructureType.VECTOR).isEmpty(), "undo whole batch");
        session.redo(); check(session.simulator().values(StructureType.VECTOR).size() == 4, "redo whole batch");
        try { session.execute(StructureType.VECTOR, Action.VECTOR_ADD, "1 2 3 4 5 6 7", null); throw new AssertionError("overflow accepted"); }
        catch (IllegalArgumentException expected) { check(session.simulator().values(StructureType.VECTOR).size() == 4, "failed batch atomic"); }
        session.execute(StructureType.LINKED_LIST, Action.LIST_ADD_AT, "8 9 10", 0);
        check(session.simulator().values(StructureType.LINKED_LIST).equals(List.of(8, 9, 10)), "batch position order");
        session.loadData(StructureType.SORTING, List.of(5, -4, 0, 12));
        session.execute(StructureType.SORTING, Action.SORT_INSERTION, "", null);
        check(session.simulator().values(StructureType.SORTING).equals(List.of(-4, 0, 5, 12)), "custom sorting dataset");
        session.clear(StructureType.SORTING);
        check(session.simulator().values(StructureType.SORTING).isEmpty(), "clear sorting");
        session.execute(StructureType.BST, Action.TREE_ADD, "8 3 10 1 6", null);
        var tree = session.simulator().current(StructureType.BST).tree();
        session.execute(StructureType.BST, Action.TREE_REMOVE, "8", null);
        session.undo(); check(session.simulator().current(StructureType.BST).tree().equals(tree), "undo exact tree shape");
        session.loadData(StructureType.LINKED_LIST, List.of(5, 8, 5, 9));
        session.execute(StructureType.LINKED_LIST, Action.LIST_REMOVE_AT, "", 2);
        check(session.simulator().values(StructureType.LINKED_LIST).equals(List.of(5, 8, 9)), "remove selected duplicate node");
        session.undo(); check(session.simulator().values(StructureType.LINKED_LIST).equals(List.of(5, 8, 5, 9)), "undo positional remove");
        session.replaceValue(StructureType.LINKED_LIST, 2, -4);
        check(session.simulator().values(StructureType.LINKED_LIST).equals(List.of(5, 8, -4, 9)), "edit selected duplicate node");
        session.replaceValue(StructureType.BST, 0, 9);
        check(session.simulator().values(StructureType.BST).equals(List.of(3, 6, 8, 9, 10)), "tree edit preserves search order");
        var beforeDuplicate = session.simulator().current(StructureType.BST).tree();
        try { session.replaceValue(StructureType.BST, 0, 8); throw new AssertionError("duplicate tree value accepted"); }
        catch (IllegalArgumentException expected) { check(session.simulator().current(StructureType.BST).tree().equals(beforeDuplicate), "invalid tree edit atomic"); }
        session.generate(StructureType.BST, 20, -10, 9, 1234);
        check(session.simulator().values(StructureType.BST).equals(java.util.stream.IntStream.range(-10, 10).boxed().toList()), "generated tree unique and bounded");
        session.undo(); check(session.simulator().current(StructureType.BST).tree().equals(beforeDuplicate), "undo generator");
        try { session.generate(StructureType.BST, 5, 0, 1, 42); throw new AssertionError("impossible generation accepted"); }
        catch (IllegalArgumentException expected) { check(true, "generator rejects too-small range"); }
        session.generate(StructureType.SORTING, 20, -1, -1, 42);
        check(session.simulator().values(StructureType.SORTING).equals(java.util.Collections.nCopies(20, -1)), "non-tree generation supports duplicates");
    }
    private static void ui(Path output) throws Exception {
        Theme.install();
        Workbench workbench = new Workbench();
        try {
            for (int[] size : new int[][] {{900, 620}, {1024, 700}, {1280, 720}, {1440, 900}}) {
                for (StructureType type : StructureType.values()) {
                    workbench.setType(type); workbench.loadExample();
                    BufferedImage image = render(workbench, size[0], size[1]);
                    if (output != null) ImageIO.write(image, "png", output.resolve("latest.png").toFile());
                    Rectangle canvasArea = new Rectangle(0, 0, workbench.canvas().getWidth(), workbench.canvas().getHeight());
                    check(canvasArea.width > 200 && canvasArea.height > 100, "canvas usable " + size[0] + " " + type + " " + canvasArea);
                    for (Rectangle2D bounds : workbench.canvas().elementBounds()) check(canvasArea.contains(bounds), "element clipped " + size[0] + " " + type + " " + bounds);
                    JButton execute = (JButton)named(workbench, "execute");
                    Rectangle button = SwingUtilities.convertRectangle(execute.getParent(), execute.getBounds(), workbench);
                    check(new Rectangle(0, 0, size[0], size[1]).contains(button), "execute button clipped");
                    check(execute.getWidth() >= execute.getPreferredSize().width, "execute label clipped");
                    JComboBox<?> operations = (JComboBox<?>)named(workbench, "operation");
                    for (int i = 0; i < operations.getItemCount(); i++) {
                        operations.setSelectedIndex(i); render(workbench, size[0], size[1]);
                        Rectangle actionBounds = SwingUtilities.convertRectangle(execute.getParent(), execute.getBounds(), workbench);
                        check(new Rectangle(0, 0, size[0], size[1]).contains(actionBounds), "operation form clipped: " + operations.getSelectedItem());
                        check(execute.getWidth() >= execute.getPreferredSize().width, "operation label clipped: " + operations.getSelectedItem());
                    }
                    operations.setSelectedIndex(0);
                    if (output != null && (type == StructureType.BST || type == StructureType.VECTOR || type == StructureType.SORTING))
                        ImageIO.write(image, "png", output.resolve(type.name().toLowerCase() + "-" + size[0] + ".png").toFile());
                }
            }
            workbench.setType(StructureType.VECTOR);
            workbench.session().clear(StructureType.VECTOR);
            workbench.setType(StructureType.VECTOR);
            workbench.runOperation(Action.VECTOR_ADD, "4 9 12", null, false);
            render(workbench, 1280, 720);
            Rectangle2D first = workbench.canvas().elementBounds().get(0);
            workbench.canvas().dispatchEvent(new MouseEvent(workbench.canvas(), MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, (int)first.getCenterX(), (int)first.getCenterY(), 1, false, MouseEvent.BUTTON1));
            check(((JTextField)named(workbench, "values")).getText().equals("4"), "canvas selection fills input");
            workbench.runOperation(Action.VECTOR_REMOVE, "", 0, false);
            workbench.undo(); check(workbench.session().simulator().values(StructureType.VECTOR).equals(List.of(4, 9, 12)), "UI undo");
            for (StructureType type : StructureType.values()) {
                if (type == StructureType.SORTING) continue;
                workbench.setType(type); workbench.loadExample(); workbench.canvas().arrange();
                render(workbench, 1280, 720);
                List<Integer> before = workbench.session().simulator().values(type);
                Rectangle2D block = workbench.canvas().elementBounds().get(0);
                int x = (int)block.getCenterX(), y = (int)block.getCenterY();
                int dx = x < workbench.canvas().getWidth() / 2 ? 40 : -40;
                int dy = y < workbench.canvas().getHeight() / 2 ? 30 : -30;
                long time = System.currentTimeMillis();
                workbench.canvas().dispatchEvent(new MouseEvent(workbench.canvas(), MouseEvent.MOUSE_PRESSED, time, 0, x, y, 1, false, MouseEvent.BUTTON1));
                workbench.canvas().dispatchEvent(new MouseEvent(workbench.canvas(), MouseEvent.MOUSE_DRAGGED, time + 1, MouseEvent.BUTTON1_DOWN_MASK, x + dx, y + dy, 0, false, MouseEvent.NOBUTTON));
                workbench.canvas().dispatchEvent(new MouseEvent(workbench.canvas(), MouseEvent.MOUSE_RELEASED, time + 2, 0, x + dx, y + dy, 1, false, MouseEvent.BUTTON1));
                render(workbench, 1280, 720);
                check(workbench.canvas().hasManualPositions(), "drag recorded " + type);
                Rectangle2D after = workbench.canvas().elementBounds().get(0);
                check(Math.abs(after.getX() - block.getX()) + Math.abs(after.getY() - block.getY()) > 5, "drag changes position " + type);
                check(workbench.session().simulator().values(type).equals(before), "drag preserves data " + type);
                render(workbench, 900, 620);
                Rectangle resized = new Rectangle(0, 0, workbench.canvas().getWidth(), workbench.canvas().getHeight());
                for (Rectangle2D bounds : workbench.canvas().elementBounds()) check(resized.contains(bounds), "dragged node survives resize " + type);
                ((JButton)named(workbench, "arrange")).doClick();
                check(!workbench.canvas().hasManualPositions(), "arrange restores layout " + type);
            }
            for (StructureType type : StructureType.values()) {
                workbench.setType(type);
                workbench.session().loadData(type, java.util.stream.IntStream.range(0, type == StructureType.VECTOR ? 10 : 20).boxed().toList());
                workbench.setType(type);
                render(workbench, 1024, 700);
                Rectangle area = new Rectangle(0, 0, workbench.canvas().getWidth(), workbench.canvas().getHeight());
                check(workbench.canvas().elementBounds().size() == (type == StructureType.VECTOR ? 10 : 20), "all dense nodes rendered");
                for (Rectangle2D bounds : workbench.canvas().elementBounds()) check(area.contains(bounds), "dense node clipped " + type);
            }
            for (int capacity : new int[]{1,5,24,64}) {
                workbench.session().configureVector(capacity,true); workbench.setType(StructureType.VECTOR);
                render(workbench,1280,720);
                check(workbench.canvas().elementBounds().size()==capacity,"all vector slots rendered: "+capacity);
                Rectangle area = new Rectangle(0,0,workbench.canvas().getWidth(),workbench.canvas().getHeight());
                for (Rectangle2D bounds : workbench.canvas().elementBounds()) check(area.contains(bounds), "custom capacity slot clipped");
            }
            workbench.showHome(); BufferedImage home = render(workbench,1440,900);
            BufferedImage smallHome = render(workbench,900,620);
            workbench.setType(StructureType.BST); workbench.showLearning();
            BufferedImage learn = render(workbench,1280,800);
            BufferedImage smallLearn = render(workbench,900,620);
            ((JComboBox<?>)named(workbench,"learningStructure")).setSelectedItem(StructureType.QUEUE);
            check(((JComboBox<?>)named(workbench,"operation")).getItemAt(0)==Action.QUEUE_ADD,"learning selector updates current structure");
            workbench.session().configureVector(12,true); workbench.setType(StructureType.VECTOR); workbench.showLaboratory();
            workbench.runOperation(Action.VECTOR_ADD,"12 7 24",null,false);
            BufferedImage source = render(workbench,1440,900);
            check(workbench.canvas().getWidth()>300,"canvas keeps usable width after compact page changes");
            if (output != null) {
                ImageIO.write(home,"png",output.resolve("home.png").toFile()); ImageIO.write(smallHome,"png",output.resolve("home-900.png").toFile());
                ImageIO.write(learn,"png",output.resolve("learn.png").toFile()); ImageIO.write(smallLearn,"png",output.resolve("learn-900.png").toFile());
                ImageIO.write(source,"png",output.resolve("code.png").toFile());
            }
        } finally { workbench.dispose(); }
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        editing();
        Path output = args.length == 0 ? null : Path.of(args[0]);
        if (output != null) Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> { try { ui(output); } catch (Exception error) { throw new RuntimeException(error); } });
        System.out.println("OK: " + checks + " checks of bulk edits, undo, full layouts, selection and clipping.");
    }
}
