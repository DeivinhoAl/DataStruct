package datastruct;

import datastruct.ui.AppFrame;
import datastruct.ui.Theme;
import javax.swing.SwingUtilities;

public final class DataStructApp {
    private DataStructApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new AppFrame().setVisible(true);
        });
    }
}
