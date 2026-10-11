package datastruct.core;

public enum Action {
    VECTOR_ADD("Inserir ao fim", true, false),
    VECTOR_REMOVE("Remover na posição", false, true),
    VECTOR_SEARCH("Buscar valor", true, false),
    VECTOR_UPDATE("Atualizar posição", true, true),
    LIST_ADD_FIRST("Inserir no início", true, false),
    LIST_ADD_LAST("Inserir no fim", true, false),
    LIST_ADD_AT("Inserir na posição", true, true),
    LIST_REMOVE("Remover valor", true, false),
    LIST_REMOVE_AT("Remover na posição", false, true),
    LIST_SEARCH("Buscar valor", true, false),
    STACK_PUSH("Empilhar", true, false),
    STACK_POP("Desempilhar", false, false),
    STACK_PEEK("Consultar topo", false, false),
    QUEUE_ADD("Enfileirar", true, false),
    QUEUE_REMOVE("Desenfileirar", false, false),
    QUEUE_PEEK("Consultar início", false, false),
    TREE_ADD("Inserir", true, false),
    TREE_REMOVE("Remover", true, false),
    TREE_SEARCH("Buscar", true, false),
    TREE_INORDER("Percorrer em ordem", false, false),
    SORT_BUBBLE("Ordenar por bolha", false, false),
    SORT_INSERTION("Ordenar por inserção", false, false),
    SORT_SHUFFLE("Embaralhar valores", false, false);

    public final String label;
    public final boolean needsValue;
    public final boolean needsPosition;

    Action(String label, boolean needsValue, boolean needsPosition) {
        this.label = label;
        this.needsValue = needsValue;
        this.needsPosition = needsPosition;
    }

    @Override public String toString() { return label; }

    public static Action[] forType(StructureType type) {
        return switch (type) {
            case VECTOR -> new Action[] { VECTOR_ADD, VECTOR_REMOVE, VECTOR_SEARCH, VECTOR_UPDATE };
            case LINKED_LIST -> new Action[] { LIST_ADD_FIRST, LIST_ADD_LAST, LIST_ADD_AT, LIST_REMOVE, LIST_REMOVE_AT, LIST_SEARCH };
            case STACK -> new Action[] { STACK_PUSH, STACK_POP, STACK_PEEK };
            case QUEUE -> new Action[] { QUEUE_ADD, QUEUE_REMOVE, QUEUE_PEEK };
            case BST -> new Action[] { TREE_ADD, TREE_REMOVE, TREE_SEARCH, TREE_INORDER };
            case SORTING -> new Action[] { SORT_BUBBLE, SORT_INSERTION, SORT_SHUFFLE };
        };
    }
}
