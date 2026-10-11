package datastruct.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Atomic editing and undo are independent of animation playback. */
public final class StudioSession {
    private record State(Map<StructureType, List<Integer>> values, int capacity) { }
    private Simulator simulator = new Simulator();
    private final Deque<State> undo = new ArrayDeque<>();
    private final Deque<State> redo = new ArrayDeque<>();

    public Simulator simulator() { return simulator; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public static boolean allowsBatch(Action action) {
        return switch (action) {
            case VECTOR_ADD, LIST_ADD_FIRST, LIST_ADD_LAST, LIST_ADD_AT, STACK_PUSH, QUEUE_ADD, TREE_ADD -> true;
            default -> false;
        };
    }

    public static List<Integer> parseValues(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Digite um valor. Exemplo: 12 ou 12, 7, 24.");
        if (trimmed.length() > 1000) throw new IllegalArgumentException("Use no máximo 64 valores por vez.");
        List<Integer> values = new ArrayList<>();
        for (String token : trimmed.split("[\\s,;]+")) {
            try {
                int value = Integer.parseInt(token);
                if (value < -999 || value > 999) throw new NumberFormatException();
                values.add(value);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("“" + token + "” não é um inteiro entre -999 e 999.");
            }
        }
        if (values.isEmpty()) throw new IllegalArgumentException("Digite ao menos um número. Exemplo: 12, 7, 24.");
        if (values.size() > 64) throw new IllegalArgumentException("Use no máximo 64 valores por vez.");
        return List.copyOf(values);
    }

    public Simulation execute(StructureType type, Action action, String text, Integer position) {
        List<Integer> values = action.needsValue ? parseValues(text) : java.util.Collections.singletonList(null);
        if (!allowsBatch(action) && values.size() > 1)
            throw new IllegalArgumentException("Esta operação usa apenas um valor.");
        State before = snapshot();
        Simulator candidate = restore(before);
        List<Step> steps = new ArrayList<>();
        Simulation last = null;
        int offset = 0;
        for (Integer value : values) {
            Integer stepPosition = position;
            if (action == Action.LIST_ADD_AT && position != null) stepPosition = position + offset++;
            last = candidate.perform(type, action, value, stepPosition);
            steps.addAll(last.steps());
        }
        if (!same(before, candidate)) checkpoint(before);
        simulator = candidate;
        return new Simulation(action, steps, last.complexity(), values.size() > 1
                ? values.size() + " valores processados na ordem digitada." : last.outcome());
    }

    public void loadData(StructureType type, List<Integer> values) {
        State before = snapshot();
        Simulator candidate = restore(before);
        candidate.load(type, values);
        if (!same(before, candidate)) checkpoint(before);
        simulator = candidate;
    }

    public void example(StructureType type) {
        Simulator example = new Simulator();
        example.example(type);
        List<Integer> values = example.storageValues(type);
        if (type == StructureType.VECTOR) values = values.subList(0, Math.min(values.size(), simulator.vectorCapacity()));
        loadData(type, values);
    }

    public void clear(StructureType type) { loadData(type, List.of()); }

    public void configureVector(int capacity, boolean empty) {
        State before = snapshot(); Simulator candidate = restore(before);
        if (empty) candidate.load(StructureType.VECTOR, List.of());
        candidate.resizeVector(capacity);
        if (!same(before, candidate)) checkpoint(before);
        simulator = candidate;
    }

    /** Edit a value without changing the order of a list, queue or stack. BST replacement removes then inserts. */
    public void replaceValue(StructureType type, int index, int value) {
        if (value < -999 || value > 999) throw new IllegalArgumentException("Use um inteiro entre -999 e 999.");
        List<Integer> values = new ArrayList<>(simulator.values(type));
        if (index < 0 || index >= values.size()) throw new IllegalArgumentException("Selecione um elemento existente.");
        if (values.get(index) == value) return;
        State before = snapshot();
        Simulator candidate = restore(before);
        if (type == StructureType.BST) {
            if (values.contains(value)) throw new IllegalArgumentException("Este valor já existe na árvore. Use um valor diferente.");
            candidate.perform(type, Action.TREE_REMOVE, values.get(index), null);
            candidate.perform(type, Action.TREE_ADD, value, null);
        } else {
            values.set(index, value);
            candidate.load(type, values);
        }
        checkpoint(before); simulator = candidate;
    }

    public void generate(StructureType type, int count, int min, int max, long seed) {
        int limit = type == StructureType.VECTOR ? simulator.vectorCapacity() : 20;
        if (count < 0 || count > limit) throw new IllegalArgumentException("Escolha de 0 a " + limit + " elementos.");
        if (min < -999 || max > 999 || min > max) throw new IllegalArgumentException("O intervalo deve estar entre -999 e 999, com mínimo menor ou igual ao máximo.");
        if (type == StructureType.BST && max - min + 1 < count)
            throw new IllegalArgumentException("O intervalo precisa ter valores diferentes suficientes para a árvore.");
        java.util.Random random = new java.util.Random(seed);
        List<Integer> result = new ArrayList<>();
        while (result.size() < count) {
            int value = min + random.nextInt(max - min + 1);
            if (type != StructureType.BST || !result.contains(value)) result.add(value);
        }
        loadData(type, result);
    }

    public void replace(Simulator loaded) {
        checkpoint(snapshot());
        simulator = restore(snapshotOf(loaded));
    }

    public void undo() {
        if (!canUndo()) return;
        redo.push(snapshot());
        simulator = restore(undo.pop());
    }

    public void redo() {
        if (!canRedo()) return;
        undo.push(snapshot());
        simulator = restore(redo.pop());
    }

    private void checkpoint(State state) {
        undo.push(state);
        if (undo.size() > 60) undo.removeLast();
        redo.clear();
    }

    private State snapshot() { return snapshotOf(simulator); }

    private static State snapshotOf(Simulator simulator) {
        var result = new EnumMap<StructureType, List<Integer>>(StructureType.class);
        for (StructureType type : StructureType.values()) result.put(type, List.copyOf(simulator.storageValues(type)));
        return new State(Map.copyOf(result), simulator.vectorCapacity());
    }

    private static boolean same(State state, Simulator simulator) {
        return state.equals(snapshotOf(simulator));
    }

    private static Simulator restore(State state) {
        Simulator simulator = new Simulator();
        simulator.resizeVector(state.capacity());
        state.values().forEach(simulator::load);
        return simulator;
    }
}
