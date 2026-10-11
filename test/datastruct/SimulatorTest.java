package datastruct;

import datastruct.core.Action;
import datastruct.core.Lessons;
import datastruct.core.SessionXml;
import datastruct.core.Simulation;
import datastruct.core.Simulator;
import datastruct.core.Step;
import datastruct.core.StructureType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free checks of the observable behavior and session format. */
public final class SimulatorTest {
    private static int checks;

    private static void equal(Object actual, Object expected, String message) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action, String message) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }

    private static void vectorAndList() {
        Simulator simulator = new Simulator();
        for (int i = 0; i < 10; i++) simulator.perform(StructureType.VECTOR, Action.VECTOR_ADD, i, null);
        rejects(() -> simulator.perform(StructureType.VECTOR, Action.VECTOR_ADD, 11, null), "full vector must reject insertion");
        simulator.perform(StructureType.VECTOR, Action.VECTOR_REMOVE, null, 3);
        equal(simulator.values(StructureType.VECTOR), List.of(0, 1, 2, 4, 5, 6, 7, 8, 9), "vector shift");
        simulator.perform(StructureType.VECTOR, Action.VECTOR_UPDATE, 99, 0);
        equal(simulator.values(StructureType.VECTOR).get(0), 99, "direct update");

        simulator.perform(StructureType.LINKED_LIST, Action.LIST_ADD_FIRST, 5, null);
        simulator.perform(StructureType.LINKED_LIST, Action.LIST_ADD_LAST, 8, null);
        Simulation inserted = simulator.perform(StructureType.LINKED_LIST, Action.LIST_ADD_AT, 6, 1);
        equal(simulator.values(StructureType.LINKED_LIST), List.of(5, 6, 8), "linked insertion");
        equal(inserted.steps().get(0).values(), List.of(5, 8), "trace keeps the earlier state");
        simulator.perform(StructureType.LINKED_LIST, Action.LIST_REMOVE, 5, null);
        equal(simulator.values(StructureType.LINKED_LIST), List.of(6, 8), "remove head");
        simulator.perform(StructureType.LINKED_LIST, Action.LIST_REMOVE, 999, null);
        equal(simulator.values(StructureType.LINKED_LIST), List.of(6, 8), "absent value preserves list");
    }

    private static void stackAndQueue() {
        Simulator simulator = new Simulator();
        rejects(() -> simulator.perform(StructureType.STACK, Action.STACK_POP, null, null), "empty stack pop");
        rejects(() -> simulator.perform(StructureType.QUEUE, Action.QUEUE_REMOVE, null, null), "empty queue remove");
        simulator.perform(StructureType.STACK, Action.STACK_PUSH, 1, null);
        simulator.perform(StructureType.STACK, Action.STACK_PUSH, 2, null);
        equal(simulator.values(StructureType.STACK), List.of(2, 1), "stack top order");
        simulator.perform(StructureType.STACK, Action.STACK_POP, null, null);
        equal(simulator.values(StructureType.STACK), List.of(1), "stack LIFO");
        simulator.perform(StructureType.QUEUE, Action.QUEUE_ADD, 1, null);
        simulator.perform(StructureType.QUEUE, Action.QUEUE_ADD, 2, null);
        simulator.perform(StructureType.QUEUE, Action.QUEUE_REMOVE, null, null);
        equal(simulator.values(StructureType.QUEUE), List.of(2), "queue FIFO");
        simulator.perform(StructureType.QUEUE, Action.QUEUE_REMOVE, null, null);
        simulator.perform(StructureType.QUEUE, Action.QUEUE_ADD, 3, null);
        equal(simulator.values(StructureType.QUEUE), List.of(3), "queue tail after becoming empty");
    }

    private static void tree() {
        Simulator simulator = new Simulator();
        simulator.example(StructureType.BST);
        equal(simulator.values(StructureType.BST), List.of(1, 3, 4, 6, 7, 8, 10, 13, 14), "BST in-order values");
        Step.TreeNode original = simulator.current(StructureType.BST).tree();
        simulator.perform(StructureType.BST, Action.TREE_ADD, 8, null);
        equal(simulator.current(StructureType.BST).tree(), original, "duplicate must preserve tree");
        simulator.perform(StructureType.BST, Action.TREE_REMOVE, 7, null); // leaf
        simulator.perform(StructureType.BST, Action.TREE_REMOVE, 14, null); // one child
        simulator.perform(StructureType.BST, Action.TREE_REMOVE, 3, null); // two children
        equal(simulator.values(StructureType.BST), List.of(1, 4, 6, 8, 10, 13), "BST deletion cases");
        simulator.perform(StructureType.BST, Action.TREE_REMOVE, 8, null); // root
        equal(simulator.values(StructureType.BST), List.of(1, 4, 6, 10, 13), "BST root deletion");
        yes(simulator.perform(StructureType.BST, Action.TREE_SEARCH, 13, null).outcome().contains("encontrado"), "BST search");
        yes(simulator.perform(StructureType.BST, Action.TREE_INORDER, null, null).outcome().contains("1, 4, 6, 10, 13"), "BST traversal");
        simulator.perform(StructureType.BST, Action.TREE_ADD, -1, null);
        equal(simulator.current(StructureType.BST).focus(), Integer.MIN_VALUE, "no tree focus sentinel");
        yes(simulator.perform(StructureType.BST, Action.TREE_SEARCH, -1, null).steps().stream()
                .anyMatch(step -> step.focus() == -1 && step.title().equals("Encontrado")), "negative tree value focus");
    }

    private static void sorting() {
        for (Action action : new Action[] { Action.SORT_BUBBLE, Action.SORT_INSERTION }) {
            Simulator simulator = new Simulator();
            simulator.load(StructureType.SORTING, List.of(5, -1, 5, 0, 9, 3, 2, -2));
            Simulation trace = simulator.perform(StructureType.SORTING, action, null, null);
            equal(simulator.values(StructureType.SORTING), List.of(-2, -1, 0, 2, 3, 5, 5, 9), action + " must sort");
            yes(trace.steps().size() > 8, action + " must expose intermediate decisions");
        }
    }

    private static void xml() throws Exception {
        Simulator simulator = new Simulator();
        simulator.load(StructureType.VECTOR, List.of(1, 2));
        simulator.load(StructureType.STACK, List.of(9, 8));
        simulator.load(StructureType.BST, List.of(8, 3, 10, 1, 6));
        Path file = Files.createTempFile("datastruct-test-", ".dsxml");
        try {
            SessionXml.save(simulator, file);
            Simulator restored = SessionXml.load(file);
            for (StructureType type : StructureType.values())
                equal(restored.values(type), simulator.values(type), "session round trip " + type);
            equal(restored.current(StructureType.BST).tree(), simulator.current(StructureType.BST).tree(), "tree shape round trip");
            Files.writeString(file, "<!DOCTYPE foo [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><datastruct version='1'>&x;</datastruct>");
            checks++;
            try { SessionXml.load(file); throw new AssertionError("DOCTYPE must be rejected"); }
            catch (org.xml.sax.SAXParseException expected) { }
        } finally { Files.deleteIfExists(file); }
    }

    public static void main(String[] args) throws Exception {
        vectorAndList();
        stackAndQueue();
        tree();
        sorting();
        xml();
        System.out.println("OK: " + checks + " verificacoes de modelos, etapas e sessao XML.");
    }
}
