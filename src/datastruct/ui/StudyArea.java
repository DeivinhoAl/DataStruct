package datastruct.ui;

import java.awt.*;
import javax.swing.*;

/** Side by side when space permits; compact tabs retain the same live components. */
public final class StudyArea extends JPanel {
    private final JComponent visualization, code;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private Boolean wide;
    private int lastWidth = -1;
    public StudyArea(JComponent visualization, JComponent code) {
        super(new BorderLayout()); this.visualization = visualization; this.code = code;
        setOpaque(false); split.setBorder(null); split.setDividerSize(9); split.setResizeWeight(.58);
        split.setContinuousLayout(true); split.setOpaque(false);
        visualization.setMinimumSize(new Dimension(250, 100)); code.setMinimumSize(new Dimension(260, 100));
    }
    @Override public void doLayout() {
        boolean next = getWidth() >= 760;
        boolean changed = wide == null || wide != next;
        if (changed) {
            removeAll(); tabs.removeAll(); split.setLeftComponent(null); split.setRightComponent(null);
            if (next) { split.setLeftComponent(visualization); split.setRightComponent(code); add(split); }
            else { tabs.addTab("Visualização", visualization); tabs.addTab("Código Java", code); add(tabs); }
            wide = next; revalidate();
        }
        super.doLayout();
        if (next && (changed || lastWidth != getWidth() || split.getDividerLocation() < 300 || split.getDividerLocation() > getWidth()-280))
            split.setDividerLocation((int)((getWidth()-split.getDividerSize())*.57));
        lastWidth = getWidth();
    }
    public void showCode() { if (Boolean.FALSE.equals(wide)) tabs.setSelectedIndex(1); code.requestFocusInWindow(); }
    public void showVisualization() { if (Boolean.FALSE.equals(wide)) tabs.setSelectedIndex(0); }
}
