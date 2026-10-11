package datastruct.ui;

import datastruct.core.Action;
import datastruct.core.Step;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

/** Displays the bundled Simulator source; highlights the actual trace emission line. */
public final class SourcePanel extends JPanel {
    private final JTextPane editor = new JTextPane() {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final JLabel method = Theme.label("Simulator.java", 12, true);
    private final List<String> lines;
    private int activeLine = -1;
    private String visibleMethod = "";
    public SourcePanel() {
        super(new BorderLayout(0, 10));
        setBackground(new Color(250, 249, 254));
        setBorder(new EmptyBorder(13, 12, 12, 12));
        JPanel header = Theme.panel(new BorderLayout());
        header.add(Theme.label("Código Java", 15, true), BorderLayout.WEST);
        JLabel badge = Theme.label("</>", 15, true); badge.setForeground(Theme.ACCENT); header.add(badge, BorderLayout.EAST);
        JPanel top = Theme.panel(new BorderLayout(0, 7)); top.add(header, BorderLayout.NORTH); top.add(method, BorderLayout.SOUTH);
        method.setForeground(Theme.MUTED); add(top, BorderLayout.NORTH);
        method.setToolTipText("A linha destacada registra o quadro atual no código real do simulador.");
        editor.setEditable(false); editor.setFont(new Font("Consolas", Font.PLAIN, 12));
        editor.setBackground(getBackground()); editor.setBorder(new EmptyBorder(6, 0, 10, 8));
        JScrollPane scroll = new JScrollPane(editor); scroll.setBorder(null); scroll.getViewport().setBackground(getBackground());
        add(scroll, BorderLayout.CENTER);
        setMinimumSize(new Dimension(260, 120));
        List<String> source;
        try {
            var input = SourcePanel.class.getResourceAsStream("/source/Simulator.java");
            if (input != null) { try (input) { source = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList(); } }
            else source = Files.readAllLines(Path.of("src/datastruct/core/Simulator.java"), StandardCharsets.UTF_8);
        } catch (Exception error) { source = List.of("// Código-fonte indisponível. Recompile com build.ps1."); }
        lines = source;
    }
    public int activeSourceLine() { return activeLine; }
    public String visibleMethod() { return visibleMethod; }
    public String displayedText() { return editor.getText(); }
    public void show(Action action, Step step) {
        String name = step != null && !step.sourceMethod().isBlank() && !step.sourceMethod().equals("perform")
                ? step.sourceMethod() : methodFor(action);
        int start = -1, end = lines.size();
        Pattern declaration = Pattern.compile("^    (?:private|public) .*\\b" + name + "\\(.*");
        for (int i = 0; i < lines.size(); i++) if (declaration.matcher(lines.get(i)).matches()) { start = i; break; }
        if (start < 0) { editor.setText("// Fonte não encontrada: " + name); activeLine = -1; return; }
        for (int i = start + 1; i < lines.size(); i++) if (lines.get(i).matches("^    (?:private|public) .*")) { end = i; break; }
        while (end > start && lines.get(end - 1).isBlank()) end--;
        visibleMethod = name; method.setText("Simulator.java · " + name + "()");
        activeLine = step != null && step.sourceLine() > start && step.sourceLine() <= end ? step.sourceLine() : -1;
        StringBuilder text = new StringBuilder(); int from = -1, to = -1;
        for (int i = start; i < end; i++) {
            if (i + 1 == activeLine) from = text.length();
            String line = lines.get(i); if (line.startsWith("    ")) line = line.substring(4);
            text.append(String.format("%3d  ", i + 1)).append(line).append('\n');
            if (i + 1 == activeLine) to = text.length() - 1;
        }
        editor.setText(text.toString());
        StyledDocument document = editor.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet(); StyleConstants.setForeground(normal, Theme.INK);
        document.setCharacterAttributes(0, document.getLength(), normal, true);
        color(document, text.toString(), "\\b(private|public|int|boolean|void|return|if|else|for|while|new|null|true|false)\\b", Theme.ACCENT);
        color(document, text.toString(), "\"(?:\\\\.|[^\"\\\\])*\"", new Color(40, 119, 131));
        color(document, text.toString(), "(?m)^\\s*\\d+", new Color(149, 143, 166));
        editor.getHighlighter().removeAllHighlights();
        if (from >= 0) {
            try {
                if (activeLine > start + 1) {
                    String previous = lines.get(activeLine - 2).strip();
                    if (!previous.startsWith("add(") && previous.matches(".*(?:=|\\+\\+|--).*;")) {
                        int previousStart = text.lastIndexOf("\n", Math.max(0,from-2)) + 1;
                        editor.getHighlighter().addHighlight(previousStart,from-1,new DefaultHighlighter.DefaultHighlightPainter(new Color(240,230,253)));
                    }
                }
                editor.getHighlighter().addHighlight(from, to, new DefaultHighlighter.DefaultHighlightPainter(new Color(229, 213, 255)));
            }
            catch (BadLocationException error) { throw new IllegalStateException(error); }
            editor.setCaretPosition(from);
        } else editor.setCaretPosition(0);
    }
    private static void color(StyledDocument doc, String text, String expression, Color color) {
        var matcher = Pattern.compile(expression).matcher(text);
        SimpleAttributeSet style = new SimpleAttributeSet(); StyleConstants.setForeground(style, color);
        while (matcher.find()) doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
    }
    private static String methodFor(Action action) {
        return switch (action) {
            case VECTOR_ADD -> "vectorAdd"; case VECTOR_REMOVE -> "vectorRemove"; case VECTOR_UPDATE -> "vectorUpdate";
            case VECTOR_SEARCH, LIST_SEARCH -> "linearSearch";
            case LIST_ADD_FIRST -> "listAddFirst"; case LIST_ADD_LAST -> "listAddLast"; case LIST_ADD_AT -> "listAddAt";
            case LIST_REMOVE -> "listRemove"; case LIST_REMOVE_AT -> "listRemoveAt";
            case STACK_PUSH -> "stackPush"; case STACK_POP -> "stackPop"; case STACK_PEEK -> "stackPeek";
            case QUEUE_ADD -> "queueAdd"; case QUEUE_REMOVE -> "queueRemove"; case QUEUE_PEEK -> "queuePeek";
            case TREE_ADD -> "treeAdd"; case TREE_REMOVE -> "treeRemove"; case TREE_SEARCH -> "treeSearch"; case TREE_INORDER -> "treeInorder";
            case SORT_BUBBLE -> "bubbleSort"; case SORT_INSERTION -> "insertionSort"; case SORT_SHUFFLE -> "shuffle";
        };
    }
}
