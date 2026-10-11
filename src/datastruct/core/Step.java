package datastruct.core;

import java.util.List;

public record Step(List<Integer> values, TreeNode tree, int focus, int secondary,
                   String title, String explanation, int codeLine, int capacity,
                   String sourceMethod, int sourceLine) {
    public Step {
        values = List.copyOf(values);
    }
    public record TreeNode(int value, TreeNode left, TreeNode right) { }
    public Step withSource(String method, int line) {
        return new Step(values, tree, focus, secondary, title, explanation, codeLine, capacity, method, line);
    }
}
