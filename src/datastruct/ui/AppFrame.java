package datastruct.ui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

public final class AppFrame extends JFrame {
    public AppFrame() {
        super("DataStruct — Laboratório de estruturas de dados");
        Workbench workbench = new Workbench();
        workbench.showHome();
        setContentPane(workbench);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(900, available.width), Math.min(620, available.height)));
        setSize(Math.min(1380, available.width - 20), Math.min(880, available.height - 20));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { workbench.dispose(); }
        });
    }
}
