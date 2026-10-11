package datastruct.ui;

import datastruct.core.*;
import datastruct.core.Action;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/** A complete workbench, renderable without a native window for layout regression tests. */
public final class Workbench extends JPanel {
    private final StudioSession session = new StudioSession();
    private StructureType type = StructureType.VECTOR;
    private Simulation simulation;
    private int stepIndex;
    private boolean updating;
    private final StructureCanvas canvas = new StructureCanvas();
    private final Map<StructureType, JButton> navigation = new EnumMap<>(StructureType.class);
    private JPanel navigationPanel;
    private final JComboBox<StructureType> compactNavigation = new JComboBox<>(StructureType.values());
    private final JLabel title = Theme.label("", 25, true);
    private final JLabel subtitle = Theme.label("", 13, false);
    private final JLabel count = Theme.label("", 12, true);
    private final JLabel counter = Theme.label("Pronto", 12, true);
    private final JTextArea explanation = Theme.prose("");
    private final JTextArea summary = Theme.prose("");
    private JDialog detailWindow;
    private final JTextArea history = Theme.prose("");
    private final JTextArea lesson = Theme.prose("");
    private final JTextArea status = Theme.prose("");
    private final JTextArea inputHint = Theme.prose("");
    private final JTextArea complexity = Theme.prose("");
    private final JTextArea selection = Theme.prose("Arraste para mover. Duplo clique edita; botão direito mostra ações.");
    private final JComboBox<Action> operation = new JComboBox<>();
    private final JTextField values = new JTextField();
    private final JSpinner position = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
    private final JPanel valueGroup = Theme.panel(new BorderLayout(0, 7));
    private final JPanel positionGroup = Theme.panel(new BorderLayout(0, 7));
    private final JLabel valueCaption = Theme.label("Valor", 13, true);
    private final JSlider timeline = new JSlider(0, 0, 0);
    private final JComboBox<String> zoom = new JComboBox<>(new String[] {"Ajustar", "50%", "75%", "100%", "125%", "150%", "200%"});
    private final JComboBox<String> speed = new JComboBox<>(new String[] {"Lenta", "Normal", "Rápida"});
    private final JCheckBox animate = new JCheckBox("Animar operação", true);
    private final JButton execute = Theme.button("Inserir ao fim", true);
    private final JButton undo = Theme.button("Desfazer", false);
    private final JButton redo = Theme.button("Refazer", false);
    private final JButton previous = Theme.button("Anterior", false);
    private final JButton play = Theme.button("Reproduzir", true);
    private final JButton next = Theme.button("Próximo", false);
    private final JButton end = Theme.button("Concluir", false);
    private final Timer timer = new Timer(650, event -> advance());
    private final JTabbedPane details = new JTabbedPane();
    private final CardLayout pagesLayout = new CardLayout();
    private final JPanel pages = new JPanel(pagesLayout);
    private final SourcePanel source = new SourcePanel();
    private final LearningPanel learning = new LearningPanel(this::showLaboratory);
    private StudyArea studyArea;
    private final JButton labTab = Theme.button("Laboratório", false);
    private final JButton learnTab = Theme.button("Aprender", false);
    private final JButton vectorSetup = Theme.button("Capacidade · 10", false);
    private String page = "lab";

    public Workbench() {
        super(new BorderLayout());
        setBackground(Theme.BG);
        pages.setBackground(Theme.BG);
        add(header(), BorderLayout.NORTH);
        navigationPanel = sidebar(); add(navigationPanel, BorderLayout.WEST);
        pages.add(workspace(), "lab"); pages.add(learning, "learn");
        pages.add(new HomePanel(selected -> { setType(selected); showLaboratory(); }), "home");
        add(pages, BorderLayout.CENTER);
        status.setRows(2);
        status.setBorder(new EmptyBorder(7, 18, 5, 18));
        add(status, BorderLayout.SOUTH);
        bind();
        learning.onSelect(this::setType);
        setType(StructureType.VECTOR);
        showLaboratory();
    }

    @Override public void doLayout() {
        if (navigationPanel != null) {
            boolean compact = getWidth() < 1120;
            navigationPanel.setVisible(!compact);
            compactNavigation.setVisible(compact);
        }
        super.doLayout();
    }

    @Override public void paint(Graphics graphics) {
        Graphics2D copy = (Graphics2D)graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(copy); copy.dispose();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), new EmptyBorder(13, 20, 13, 20)));
        JPanel brand = Theme.panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton home = Theme.button("DataStruct", false); home.setFont(Theme.BOLD.deriveFont(21f));
        home.setBorder(new EmptyBorder(4, 8, 4, 8)); home.setToolTipText("Início");
        home.addActionListener(event -> showHome()); brand.add(home);
        header.add(brand, BorderLayout.WEST);
        JPanel navigation = Theme.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        labTab.addActionListener(event -> showLaboratory()); learnTab.addActionListener(event -> showLearning());
        navigation.add(labTab); navigation.add(learnTab); header.add(navigation, BorderLayout.CENTER);
        JPanel files = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        JButton open = Theme.button("Abrir", false), save = Theme.button("Salvar", false), export = Theme.button("Exportar", false);
        open.addActionListener(event -> openSession()); save.addActionListener(event -> saveSession());
        export.addActionListener(event -> exportImage());
        files.add(open); files.add(save); files.add(export);
        header.add(files, BorderLayout.EAST);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(Theme.spectrum(), BorderLayout.NORTH); wrapper.add(header); return wrapper;
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 18));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,Theme.LINE), new EmptyBorder(23, 12, 14, 12)));
        side.setPreferredSize(new Dimension(190, 0));
        JPanel items = Theme.panel(new GridLayout(0, 1, 0, 7));
        JLabel label = Theme.label("ESTRUTURAS", 11, true); label.setForeground(Theme.MUTED);
        items.add(label);
        for (StructureType structure : StructureType.values()) {
            JButton button = Theme.button(structure.title, false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorder(new EmptyBorder(13, 9, 13, 7));
            button.setIcon(Theme.structureIcon(structure.ordinal())); button.setIconTextGap(10);
            button.addActionListener(event -> { setType(structure); if (page.equals("home")) showLaboratory(); });
            navigation.put(structure, button); items.add(button);
        }
        side.add(items, BorderLayout.NORTH);
        return side;
    }

    private JPanel workspace() {
        JPanel body = Theme.panel(new BorderLayout(14, 0));
        body.setBorder(new EmptyBorder(17, 17, 0, 17));
        JPanel lab = Theme.panel(new BorderLayout(0, 12));
        JPanel heading = Theme.panel(new BorderLayout());
        JPanel names = Theme.panel(new BorderLayout(0, 3));
        names.add(title, BorderLayout.CENTER);
        heading.add(names, BorderLayout.CENTER);
        compactNavigation.setPreferredSize(new Dimension(176, 34));
        compactNavigation.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof StructureType structure) setText(structure.title);
                return this;
            }
        });
        JPanel compact = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 0, 5)); compact.add(compactNavigation);
        heading.add(compact, BorderLayout.EAST); lab.add(heading, BorderLayout.NORTH);

        JPanel stage = new JPanel(new BorderLayout());
        stage.setBorder(new Theme.RoundBorder(Theme.LINE, 16, new Insets(1,1,1,1)));
        JPanel tools = Theme.panel(new BorderLayout(10, 0));
        tools.setBorder(new EmptyBorder(9, 12, 9, 12));
        tools.add(count, BorderLayout.WEST);
        JPanel zoomControls = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        JButton inspect = Theme.button("Histórico", false);
        inspect.setBorder(new EmptyBorder(6, 10, 6, 10));
        inspect.addActionListener(event -> openDetails()); zoomControls.add(inspect);
        JButton arrange = Theme.button("Organizar", false);
        arrange.setBorder(new EmptyBorder(6, 10, 6, 10)); arrange.setName("arrange");
        arrange.setToolTipText("Restaurar a disposição automática dos blocos");
        arrange.addActionListener(event -> { canvas.arrange(); message("Desenho organizado. Os valores e as ligações foram preservados.", false); });
        zoomControls.add(arrange);
        zoom.setToolTipText("Zoom: ajustar o desenho ou escolher uma escala");
        zoom.setPreferredSize(new Dimension(90, 29)); zoomControls.add(zoom);
        tools.add(zoomControls, BorderLayout.EAST); stage.add(tools, BorderLayout.NORTH);
        JScrollPane view = new JScrollPane(canvas); view.setBorder(null);
        view.getViewport().setBackground(Color.WHITE);
        stage.add(view, BorderLayout.CENTER);
        JPanel legend = Theme.panel(new BorderLayout());
        legend.setBorder(new EmptyBorder(6, 12, 7, 12));
        JLabel key = Theme.label("<html><span style='color:#7d4bc5'>● Ativo</span>&nbsp;&nbsp;&nbsp;<span style='color:#b87516'>● Comparação</span></html>", 11, false);
        key.setForeground(Theme.MUTED); legend.add(key);
        stage.add(legend, BorderLayout.SOUTH);

        JPanel center = Theme.panel(new BorderLayout(0, 9));
        studyArea = new StudyArea(stage, source);
        center.add(studyArea, BorderLayout.CENTER);
        center.add(playback(), BorderLayout.SOUTH);
        lab.add(center, BorderLayout.CENTER);

        explanation.setFont(Theme.BODY.deriveFont(14f));
        explanation.setBorder(new EmptyBorder(12, 14, 12, 14));
        history.setBorder(new EmptyBorder(10, 14, 10, 14));
        history.setFont(new Font("Consolas", Font.PLAIN, 12));
        details.addTab("Operação", scroll(explanation));
        details.addTab("Histórico", scroll(history));
        JPanel concepts = new JPanel(new BorderLayout(0, 16));
        concepts.setBackground(Color.WHITE);
        concepts.setBorder(new EmptyBorder(16, 16, 16, 16));
        lesson.setFont(Theme.BODY.deriveFont(15f));
        complexity.setForeground(Theme.ACCENT); complexity.setFont(Theme.BOLD);
        complexity.setRows(2);
        concepts.add(lesson, BorderLayout.CENTER); concepts.add(complexity, BorderLayout.SOUTH);
        details.addTab("Conceito e custo", scroll(concepts));
        summary.setRows(2); summary.setForeground(Theme.INK);
        summary.setBackground(Color.WHITE); summary.setOpaque(true);
        summary.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE), new EmptyBorder(10, 13, 10, 13)));
        JScrollPane summaryView = scroll(summary);
        summaryView.setPreferredSize(new Dimension(0, 59));
        summaryView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        lab.add(summaryView, BorderLayout.SOUTH);
        body.add(lab, BorderLayout.CENTER);

        JScrollPane actions = scroll(operations());
        actions.setPreferredSize(new Dimension(254, 0));
        actions.getVerticalScrollBar().setUnitIncrement(20);
        actions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        actions.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        body.add(actions, BorderLayout.EAST);
        return body;
    }

    private JPanel operations() {
        JPanel card = new FormPanel();
        card.setBorder(new EmptyBorder(16, 15, 16, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(0, 0, 8, 0);
        row(card, Theme.label("Operações", 18, true), c);
        vectorSetup.addActionListener(event -> configureVector()); row(card, vectorSetup, c);
        operation.setPreferredSize(new Dimension(200, 35)); operation.setName("operation");
        row(card, operation, c);
        valueGroup.add(valueCaption, BorderLayout.NORTH);
        values.setName("values"); values.setFont(Theme.BODY.deriveFont(15f));
        values.setBorder(new Theme.RoundBorder(Theme.LINE, 12, new Insets(10, 10, 10, 10)));
        values.getAccessibleContext().setAccessibleName("Valor ou valores para a operação");
        valueGroup.add(values, BorderLayout.CENTER); row(card, valueGroup, c);
        positionGroup.add(Theme.label("Posição (começa em 0)", 12, true), BorderLayout.NORTH);
        position.setName("position"); position.setPreferredSize(new Dimension(200, 33));
        positionGroup.add(position); row(card, positionGroup, c);
        inputHint.setRows(2); inputHint.setFont(Theme.BODY.deriveFont(12f)); row(card, inputHint, c);
        execute.setName("execute"); row(card, execute, c);

        JPanel animation = Theme.panel(new BorderLayout(4, 0));
        animate.setOpaque(false); animate.setFont(Theme.BODY.deriveFont(12f));
        speed.setPreferredSize(new Dimension(84, 28)); speed.setSelectedIndex(1);
        animation.add(animate, BorderLayout.CENTER); animation.add(speed, BorderLayout.EAST); row(card, animation, c);
        row(card, new JSeparator(), c);
        JPanel historyButtons = Theme.panel(new GridLayout(1, 2, 7, 0));
        undo.setName("undo"); redo.setName("redo"); historyButtons.add(undo); historyButtons.add(redo); row(card, historyButtons, c);
        JButton example = Theme.button("Carregar exemplo", false), edit = Theme.button("Editar todos os valores", false), clear = Theme.button("Limpar estrutura", false);
        example.addActionListener(event -> loadExample()); edit.addActionListener(event -> editData());
        clear.addActionListener(event -> { session.clear(type); resetPlayback(); log("Estrutura limpa"); message("Estrutura limpa. Você pode desfazer esta ação.", false); });
        JButton generate = Theme.button("Gerar valores…", false);
        generate.addActionListener(event -> generateData());
        JPanel datasets = Theme.panel(new GridLayout(1, 2, 7, 0));
        example.setText("Exemplo"); generate.setText("Gerar…");
        datasets.add(example); datasets.add(generate);
        row(card, datasets, c); row(card, edit, c); row(card, clear, c);
        row(card, new JSeparator(), c);
        selection.setRows(2); selection.setFont(Theme.BODY.deriveFont(11f)); row(card, selection, c);
        c.weighty = 1; row(card, Box.createVerticalGlue(), c);
        return card;
    }
    private static void row(JPanel panel, Component component, GridBagConstraints c) { panel.add(component, c); c.gridy++; }
    private static final class FormPanel extends JPanel implements Scrollable {
        FormPanel() { super(new GridBagLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(265, 650); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public int getScrollableUnitIncrement(Rectangle r, int axis, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int axis, int direction) { return 140; }
    }
    private static JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component); pane.setBorder(null); pane.getViewport().setBackground(Color.WHITE); return pane;
    }

    private JPanel playback() {
        JPanel panel = Theme.panel(new BorderLayout(0, 5));
        JPanel track = Theme.panel(new BorderLayout(9, 0));
        timeline.setOpaque(false); timeline.setMinimumSize(new Dimension(30, 25));
        track.add(timeline, BorderLayout.CENTER); track.add(counter, BorderLayout.EAST);
        panel.add(track, BorderLayout.NORTH);
        JPanel buttons = Theme.panel(new GridLayout(1, 4, 7, 0));
        buttons.add(previous); buttons.add(play); buttons.add(next); buttons.add(end);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void bind() {
        compactNavigation.addActionListener(event -> { if (!updating) setType((StructureType)compactNavigation.getSelectedItem()); });
        operation.addActionListener(event -> { if (!updating) updateForm(); });
        execute.addActionListener(event -> executeInput()); values.addActionListener(event -> executeInput());
        previous.addActionListener(event -> { timer.stop(); showStep(stepIndex - 1); });
        next.addActionListener(event -> { timer.stop(); showStep(stepIndex + 1); });
        end.addActionListener(event -> finish());
        play.addActionListener(event -> {
            if (timer.isRunning()) timer.stop();
            else if (simulation != null) { if (stepIndex == simulation.steps().size() - 1) showStep(0); timer.start(); }
            updateButtons();
        });
        timeline.addChangeListener(event -> { if (!updating && simulation != null) { timer.stop(); showStep(timeline.getValue()); } });
        zoom.addActionListener(event -> canvas.setZoom(zoom.getSelectedIndex() == 0 ? 0
                : Double.parseDouble(((String)zoom.getSelectedItem()).replace("%", "")) / 100));
        speed.addActionListener(event -> timer.setDelay(new int[] {1100, 650, 220}[speed.getSelectedIndex()]));
        undo.addActionListener(event -> undo()); redo.addActionListener(event -> redo());
        canvas.onSelect((index, value) -> {
            timer.stop(); updateButtons();
            values.setText(value == null ? "" : String.valueOf(value));
            int max = ((Number)((SpinnerNumberModel)position.getModel()).getMaximum()).intValue();
            position.setValue(Math.min(index, max));
            selection.setText(value == null ? "Vaga no índice " + index + ". Duplo clique insere na próxima vaga."
                    : "Selecionado: " + value + (type == StructureType.BST ? "" : "   ·   posição " + index) + "\nDuplo clique edita; botão direito mostra ações.");
        });
        canvas.onEdit(this::editValue);
        canvas.onContext(this::contextMenu);
        canvas.onReposition(() -> message("Posição visual alterada. A ordem e as ligações foram preservadas. Organizar restaura o desenho.", false));
        shortcut("control Z", "undo", this::undo); shortcut("control Y", "redo", this::redo);
        shortcut("control S", "save", this::saveSession); shortcut("control O", "open", this::openSession);
    }
    private void shortcut(String key, String name, Runnable action) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() { @Override public void actionPerformed(ActionEvent event) { action.run(); } });
    }

    public void setType(StructureType selected) {
        timer.stop(); type = selected; simulation = null;
        title.setText(type.title); subtitle.setText(type.subtitle); lesson.setText(type.lesson);
        learning.show(type);
        if (studyArea != null) studyArea.showVisualization();
        for (var item : navigation.entrySet()) {
            item.getValue().setBackground(item.getKey() == selected ? Theme.TINT : Color.WHITE);
            item.getValue().setForeground(item.getKey() == selected ? Theme.ACCENT : Theme.INK);
        }
        updating = true; operation.removeAllItems();
        compactNavigation.setSelectedItem(selected);
        for (Action action : Action.forType(type)) operation.addItem(action);
        updating = false; values.setText(""); zoom.setSelectedIndex(0);
        resetPlayback(); message("", false);
    }
    public void loadExample() {
        session.example(type); resetPlayback(); log("Exemplo carregado"); message("Exemplo carregado. Escolha uma operação para explorar.", false);
    }
    private void updateForm() {
        Action action = (Action)operation.getSelectedItem(); if (action == null) return;
        vectorSetup.setVisible(type == StructureType.VECTOR);
        vectorSetup.setText("Capacidade · " + session.simulator().vectorCapacity());
        valueGroup.setVisible(action.needsValue); positionGroup.setVisible(action.needsPosition);
        boolean batch = StudioSession.allowsBatch(action);
        valueCaption.setText(batch ? "Valor ou vários valores" : "Valor");
        values.setToolTipText(batch ? "Exemplo: 12, 7, 24 — inseridos na ordem digitada" : "Um número inteiro entre -999 e 999");
        int size = session.simulator().values(type).size();
        int max = Math.max(0, action == Action.LIST_ADD_AT ? size : size - 1);
        int current = Math.min(((Number)position.getValue()).intValue(), max);
        position.setModel(new SpinnerNumberModel(current, 0, max, 1));
        inputHint.setText(batch ? "Ex.: 12, 7, 24"
                : action.needsPosition ? "Escolha uma posição de 0 a " + max + ". Você também pode clicar em um elemento."
                : action.needsValue ? "Inteiro entre −999 e 999."
                : "Usa os valores da estrutura.");
        execute.setText(action.label); complexity.setText(Lessons.complexity(action));
        if (simulation == null) updateCode(action, -1);
        revalidate();
    }
    private void executeInput() {
        try {
            position.commitEdit();
            runOperation((Action)operation.getSelectedItem(), values.getText(), ((Number)position.getValue()).intValue(), animate.isSelected());
            if (StudioSession.allowsBatch((Action)operation.getSelectedItem())) { values.selectAll(); values.requestFocusInWindow(); }
        } catch (Exception error) { message(error.getMessage() == null ? "Verifique a posição informada." : error.getMessage(), true); }
    }
    public void runOperation(Action action, String input, Integer index, boolean animation) {
        if (simulation != null && stepIndex < simulation.steps().size() - 1) finish();
        Simulation result = session.execute(type, action, input, index);
        timer.stop(); simulation = result; stepIndex = 0;
        showStep(animation ? 0 : result.steps().size() - 1);
        log(action.label + (action.needsValue ? " (" + input.trim() + ")" : "") + " — " + result.outcome());
        if (animation && result.steps().size() > 1) timer.start();
        updateButtons();
    }
    private void advance() {
        if (simulation == null) { timer.stop(); return; }
        showStep(stepIndex + 1);
        if (stepIndex == simulation.steps().size() - 1) timer.stop();
        updateButtons();
    }
    public void showStep(int index) {
        if (simulation == null) return;
        stepIndex = Math.max(0, Math.min(index, simulation.steps().size() - 1));
        Step step = simulation.steps().get(stepIndex); canvas.show(type, step);
        explanation.setText(step.title() + "\n\n" + step.explanation()); explanation.setCaretPosition(0);
        summary.setText(step.title() + "\n" + step.explanation()); summary.setCaretPosition(0);
        count.setText(type == StructureType.VECTOR ? step.values().size() + " / " + step.capacity() + " posições" : step.values().size() + " elementos");
        source.show(simulation.action(), step);
        updating = true; timeline.setMaximum(simulation.steps().size() - 1); timeline.setValue(stepIndex); updating = false;
        counter.setText((stepIndex + 1) + " / " + simulation.steps().size());
        if (stepIndex == simulation.steps().size() - 1) {
            message(simulation.outcome(), false); updateForm();
        } else message("", false);
        updateButtons();
    }
    public void finish() { timer.stop(); if (simulation != null) showStep(simulation.steps().size() - 1); updateButtons(); }
    private void resetPlayback() {
        timer.stop(); simulation = null; stepIndex = 0;
        Step state = session.simulator().current(type); canvas.show(type, state);
        count.setText(type == StructureType.VECTOR ? state.values().size() + " / " + state.capacity() + " posições" : state.values().size() + " elementos");
        explanation.setText("Pronto para explorar\n\nEscolha uma operação à direita. O desenho e esta explicação acompanham cada alteração.");
        summary.setText("Escolha uma operação ou carregue um exemplo.");
        selection.setText(type == StructureType.SORTING ? "Duplo clique edita uma barra; botão direito mostra ações."
                : "Arraste para mover. Duplo clique edita; botão direito mostra ações.");
        updating = true; timeline.setMaximum(0); timeline.setValue(0); updating = false;
        counter.setText("Pronto"); updateForm(); updateButtons();
    }
    private void updateButtons() {
        boolean has = simulation != null;
        boolean active = has && stepIndex < simulation.steps().size() - 1;
        previous.setEnabled(has && stepIndex > 0); next.setEnabled(active); end.setEnabled(active);
        play.setEnabled(has); play.setText(timer.isRunning() ? "Pausar" : has && !active ? "Rever" : "Reproduzir");
        timeline.setEnabled(has); execute.setEnabled(!active); operation.setEnabled(!active);
        values.setEnabled(!active); position.setEnabled(!active);
        undo.setEnabled(session.canUndo()); redo.setEnabled(session.canRedo());
    }
    public void undo() { timer.stop(); session.undo(); resetPlayback(); log("Desfazer"); message("Estado anterior restaurado.", false); }
    public void redo() { timer.stop(); session.redo(); resetPlayback(); log("Refazer"); message("Alteração reaplicada.", false); }
    public StudioSession session() { return session; }
    public StructureCanvas canvas() { return canvas; }
    public SourcePanel sourcePanel() { return source; }
    public void showHome() { timer.stop(); page = "home"; pagesLayout.show(pages, page); updatePageTabs(); updateButtons(); }
    public void showLaboratory() { page = "lab"; pagesLayout.show(pages, page); updatePageTabs(); }
    public void showLearning() { timer.stop(); page = "learn"; pagesLayout.show(pages, page); updatePageTabs(); updateButtons(); }
    private void updatePageTabs() {
        labTab.setBackground(page.equals("lab") ? Theme.TINT : Color.WHITE); labTab.setForeground(page.equals("lab") ? Theme.ACCENT : Theme.MUTED);
        learnTab.setBackground(page.equals("learn") ? Theme.TINT : Color.WHITE); learnTab.setForeground(page.equals("learn") ? Theme.ACCENT : Theme.MUTED);
    }
    private void configureVector() {
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(session.simulator().vectorCapacity(), 1, 64, 1));
        JCheckBox empty = new JCheckBox("Começar com um vetor vazio");
        JPanel form = new JPanel(new GridLayout(0,1,0,12)); form.add(new JLabel("Capacidade do vetor (1 a 64)")); form.add(capacity); form.add(empty);
        while (JOptionPane.showConfirmDialog(this, form, "Configurar vetor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                capacity.commitEdit(); session.configureVector((int)capacity.getValue(), empty.isSelected());
                canvas.arrange(); resetPlayback(); log("Capacidade do vetor: " + capacity.getValue());
                message("Vetor configurado. A alteração pode ser desfeita.", false); return;
            } catch (Exception error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Confira a capacidade", JOptionPane.INFORMATION_MESSAGE); }
        }
    }
    public void dispose() { timer.stop(); if (detailWindow != null) detailWindow.dispose(); }

    private boolean canEditDrawing() { return simulation == null || stepIndex == simulation.steps().size() - 1; }

    private void editValue(int index, Integer current) {
        if (!canEditDrawing()) { message("Conclua a operação para editar os dados. Você pode reorganizar o desenho durante a pausa.", false); return; }
        if (current == null && index != session.simulator().values(type).size()) {
            message("O vetor é preenchido em sequência. A próxima vaga é o índice " + session.simulator().values(type).size() + ".", false); return;
        }
        String prompt = type == StructureType.BST ? "Novo valor (o nó será removido e o novo valor inserido na árvore):" : "Valor inteiro entre -999 e 999:";
        String input = (String)JOptionPane.showInputDialog(this, prompt, current == null ? "Inserir na vaga" : "Editar valor " + current,
                JOptionPane.PLAIN_MESSAGE, null, null, current == null ? "" : current.toString());
        if (input == null) return;
        try {
            List<Integer> parsed = StudioSession.parseValues(input);
            if (parsed.size() != 1) throw new IllegalArgumentException("Digite apenas um valor para este elemento.");
            if (current == null) runOperation(Action.VECTOR_ADD, input, null, animate.isSelected());
            else {
                session.replaceValue(type, index, parsed.get(0)); resetPlayback();
                log("Valor " + current + " alterado para " + parsed.get(0));
                summary.setText("Conjunto editado\n" + (type == StructureType.BST
                        ? "O valor antigo foi removido e o novo inserido, preservando a regra da árvore de busca."
                        : "O valor do elemento foi alterado. A ordem dos outros elementos foi preservada."));
                message("Valor atualizado. Desfazer recupera o estado anterior.", false);
            }
        } catch (IllegalArgumentException error) { message(error.getMessage(), true); }
    }

    private void contextMenu(StructureCanvas.Selection selected) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem edit = new JMenuItem(selected.value() == null ? "Inserir nesta vaga…" : "Editar valor…");
        edit.setEnabled(canEditDrawing() && (selected.value() != null || selected.index() == session.simulator().values(type).size()));
        edit.addActionListener(event -> editValue(selected.index(), selected.value())); menu.add(edit);
        if (selected.value() != null) {
            Action remove = switch (type) {
                case VECTOR -> Action.VECTOR_REMOVE;
                case LINKED_LIST -> Action.LIST_REMOVE_AT;
                case BST -> Action.TREE_REMOVE;
                case STACK -> selected.index() == 0 ? Action.STACK_POP : null;
                case QUEUE -> selected.index() == 0 ? Action.QUEUE_REMOVE : null;
                case SORTING -> null;
            };
            if (remove != null) {
                JMenuItem item = new JMenuItem(remove.label);
                item.setEnabled(canEditDrawing()); item.addActionListener(event -> contextualOperation(remove, selected)); menu.add(item);
            }
            Action search = switch (type) {
                case VECTOR -> Action.VECTOR_SEARCH; case LINKED_LIST -> Action.LIST_SEARCH; case BST -> Action.TREE_SEARCH; default -> null;
            };
            if (search != null) {
                JMenuItem item = new JMenuItem("Buscar este valor"); item.setEnabled(canEditDrawing());
                item.addActionListener(event -> contextualOperation(search, selected)); menu.add(item);
            }
        }
        menu.addSeparator();
        JMenuItem arrange = new JMenuItem("Organizar desenho"); arrange.addActionListener(event -> canvas.arrange()); menu.add(arrange);
        menu.show(canvas, selected.x(), selected.y());
    }

    private void contextualOperation(Action action, StructureCanvas.Selection selected) {
        operation.setSelectedItem(action);
        try { runOperation(action, String.valueOf(selected.value()), selected.index(), animate.isSelected()); }
        catch (IllegalArgumentException error) { message(error.getMessage(), true); }
    }

    private void generateData() {
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 10));
        JSpinner amount = new JSpinner(new SpinnerNumberModel(Math.min(8, type == StructureType.VECTOR ? session.simulator().vectorCapacity() : 20), 0, type == StructureType.VECTOR ? session.simulator().vectorCapacity() : 20, 1));
        JSpinner min = new JSpinner(new SpinnerNumberModel(1, -999, 999, 1));
        JSpinner max = new JSpinner(new SpinnerNumberModel(99, -999, 999, 1));
        form.add(new JLabel("Quantidade")); form.add(amount);
        form.add(new JLabel("Menor valor")); form.add(min);
        form.add(new JLabel("Maior valor")); form.add(max);
        while (JOptionPane.showConfirmDialog(this, form, "Gerar conjunto · " + type.title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                amount.commitEdit(); min.commitEdit(); max.commitEdit();
                session.generate(type, (int)amount.getValue(), (int)min.getValue(), (int)max.getValue(), System.nanoTime());
                canvas.arrange(); resetPlayback(); log("Conjunto gerado");
                message("Conjunto gerado. Use Desfazer para recuperar os valores anteriores.", false); return;
            } catch (Exception error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Confira os valores", JOptionPane.INFORMATION_MESSAGE); }
        }
    }

    private void openDetails() {
        if (detailWindow == null) {
            detailWindow = new JDialog(SwingUtilities.getWindowAncestor(this), "DataStruct · detalhes da execução", Dialog.ModalityType.MODELESS);
            detailWindow.setContentPane(details); detailWindow.setSize(650, 390);
            detailWindow.setMinimumSize(new Dimension(430, 250));
            detailWindow.setLocationRelativeTo(this);
        }
        detailWindow.setVisible(true);
    }

    private void updateCode(Action action, int active) { source.show(action, null); }
    private void message(String text, boolean error) { status.setText(text); status.setForeground(error ? new Color(178, 64, 43) : Theme.MUTED); }
    private void log(String text) {
        history.append(type.title + " · " + text + "\n");
        if (history.getDocument().getLength() > 18000) history.setText(history.getText().substring(6000));
        history.setCaretPosition(history.getDocument().getLength());
    }
    private void editData() {
        finish();
        JTextArea data = new JTextArea(5, 28); data.setLineWrap(true); data.setWrapStyleWord(true); data.setFont(Theme.BODY);
        data.setText(session.simulator().storageValues(type).toString().replace("[", "").replace("]", ""));
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        JTextArea help = Theme.prose("Separe inteiros de -999 a 999 por vírgula ou espaço.\n"
                + (type == StructureType.STACK ? "Na pilha, escreva do topo para a base."
                : type == StructureType.BST ? "Na árvore, a ordem digitada define a ordem de inserção. Valores devem ser únicos."
                : "Os valores serão exibidos na ordem digitada.") + "\nLimite: " + (type == StructureType.VECTOR ? session.simulator().vectorCapacity() : 20) + " elementos. Deixe vazio para limpar.");
        panel.add(help, BorderLayout.NORTH); panel.add(new JScrollPane(data), BorderLayout.CENTER);
        while (JOptionPane.showConfirmDialog(this, panel, "Editar " + type.title.toLowerCase(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                session.loadData(type, data.getText().isBlank() ? List.of() : StudioSession.parseValues(data.getText()));
                resetPlayback(); log("Conjunto de valores atualizado"); message("Valores atualizados. Use Desfazer para recuperar o conjunto anterior.", false); return;
            } catch (IllegalArgumentException error) { help.setText(error.getMessage()); help.setForeground(new Color(178, 64, 43)); }
        }
    }
    private JFileChooser chooser(String extension, String description) {
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter(description, extension)); return chooser;
    }
    private Path outputPath(JFileChooser chooser, String extension) {
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith("." + extension)) path = path.resolveSibling(path.getFileName() + "." + extension);
        if (path.toFile().exists() && JOptionPane.showConfirmDialog(this, "Substituir o arquivo existente?", "Salvar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
        return path;
    }
    private void saveSession() {
        finish(); Path path = outputPath(chooser("dsxml", "Sessão DataStruct"), "dsxml"); if (path == null) return;
        try { SessionXml.save(session.simulator(), path); message("Sessão salva em " + path.getFileName(), false); }
        catch (Exception error) { message("Não foi possível salvar: " + error.getMessage(), true); }
    }
    private void openSession() {
        JFileChooser chooser = chooser("dsxml", "Sessão DataStruct");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { session.replace(SessionXml.load(chooser.getSelectedFile().toPath())); resetPlayback(); log("Sessão aberta"); message("Sessão carregada. Desfazer recupera a sessão anterior.", false); }
        catch (Exception error) { message("Não foi possível abrir: " + error.getMessage(), true); }
    }
    private void exportImage() {
        timer.stop(); updateButtons(); Path path = outputPath(chooser("png", "Imagem PNG"), "png"); if (path == null) return;
        try { ImageIO.write(canvas.exportImage(), "png", path.toFile()); message("Visualização exportada para " + path.getFileName(), false); }
        catch (Exception error) { message("Não foi possível exportar: " + error.getMessage(), true); }
    }
}
