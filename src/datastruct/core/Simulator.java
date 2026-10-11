package datastruct.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** The structures live here; the Swing interface only reads immutable snapshots. */
public final class Simulator {
    private static final int MAX_NODES = 20;
    private static final int NO_TREE_FOCUS = Integer.MIN_VALUE;
    private int[] vector = new int[10];
    private int vectorSize;
    private Node listHead;
    private Node stackTop;
    private Node queueHead;
    private Node queueTail;
    private Tree treeRoot;
    private int[] sorting = { 7, 3, 9, 2, 6, 1, 8, 4 };
    private final Random random = new Random();

    public int vectorCapacity() { return vector.length; }

    public void resizeVector(int capacity) {
        require(capacity >= 1 && capacity <= 64, "Escolha uma capacidade de 1 a 64 posições.");
        require(capacity >= vectorSize, "A capacidade deve comportar os " + vectorSize + " valores atuais. Esvazie o vetor para criar um menor.");
        vector = Arrays.copyOf(vector, capacity);
    }

    private static final class Node {
        int value;
        Node next;
        Node(int value) { this.value = value; }
    }
    private static final class Tree {
        int value;
        Tree left;
        Tree right;
        Tree(int value) { this.value = value; }
    }

    public Step current(StructureType type) {
        return frame(type, type == StructureType.BST ? NO_TREE_FOCUS : -1, -1,
                "Área de simulação", "Escolha uma operação para observar cada decisão.", -1);
    }

    public List<Integer> values(StructureType type) {
        List<Integer> result = new ArrayList<>();
        switch (type) {
            case VECTOR -> { for (int i = 0; i < vectorSize; i++) result.add(vector[i]); }
            case LINKED_LIST -> copyNodes(listHead, result);
            case STACK -> copyNodes(stackTop, result);
            case QUEUE -> copyNodes(queueHead, result);
            case BST -> inorder(treeRoot, result);
            case SORTING -> { for (int value : sorting) result.add(value); }
        }
        return result;
    }

    private static void copyNodes(Node first, List<Integer> into) {
        for (Node node = first; node != null; node = node.next) into.add(node.value);
    }

    private static void inorder(Tree node, List<Integer> into) {
        if (node == null) return;
        inorder(node.left, into);
        into.add(node.value);
        inorder(node.right, into);
    }

    public List<Integer> storageValues(StructureType type) {
        if (type != StructureType.BST) return values(type);
        List<Integer> result = new ArrayList<>();
        preorder(treeRoot, result);
        return result;
    }

    private static void preorder(Tree node, List<Integer> into) {
        if (node == null) return;
        into.add(node.value);
        preorder(node.left, into);
        preorder(node.right, into);
    }

    private static Step.TreeNode copyTree(Tree node) {
        return node == null ? null : new Step.TreeNode(node.value, copyTree(node.left), copyTree(node.right));
    }

    private Step frame(StructureType type, int focus, int secondary, String title, String explanation, int codeLine) {
        return new Step(values(type), type == StructureType.BST ? copyTree(treeRoot) : null,
                focus, secondary, title, explanation, codeLine, vector.length, "", -1);
    }

    private void add(List<Step> steps, StructureType type, int focus, int secondary,
                     String title, String explanation, int line) {
        StackWalker.StackFrame caller = StackWalker.getInstance().walk(frames -> frames.skip(1).findFirst().orElseThrow());
        steps.add(frame(type, focus, secondary, title, explanation, line).withSource(caller.getMethodName(), caller.getLineNumber()));
    }

    private static void require(boolean condition, String explanation) {
        if (!condition) throw new IllegalArgumentException(explanation);
    }

    private static int needed(Integer value, String name) {
        require(value != null, "Informe " + name + ".");
        require(value >= -999 && value <= 999, "Use números entre -999 e 999 para manter a visualização legível.");
        return value;
    }

    public Simulation perform(StructureType type, Action action, Integer valueInput, Integer positionInput) {
        require(Arrays.asList(Action.forType(type)).contains(action), "Esta operação não pertence à estrutura selecionada.");
        int value = action.needsValue ? needed(valueInput, "um valor") : 0;
        int position = action.needsPosition ? needed(positionInput, "uma posição") : 0;
        List<Step> steps = new ArrayList<>();
        add(steps, type, type == StructureType.BST ? NO_TREE_FOCUS : -1, -1,
                "Antes da operação", "Observe o estado inicial e faça uma previsão antes de avançar.", -1);
        String outcome = switch (action) {
            case VECTOR_ADD -> vectorAdd(value, steps);
            case VECTOR_REMOVE -> vectorRemove(position, steps);
            case VECTOR_SEARCH -> linearSearch(type, value, steps);
            case VECTOR_UPDATE -> vectorUpdate(position, value, steps);
            case LIST_ADD_FIRST -> listAddFirst(value, steps);
            case LIST_ADD_LAST -> listAddLast(value, steps);
            case LIST_ADD_AT -> listAddAt(position, value, steps);
            case LIST_REMOVE -> listRemove(value, steps);
            case LIST_REMOVE_AT -> listRemoveAt(position, steps);
            case LIST_SEARCH -> linearSearch(type, value, steps);
            case STACK_PUSH -> stackPush(value, steps);
            case STACK_POP -> stackPop(steps);
            case STACK_PEEK -> stackPeek(steps);
            case QUEUE_ADD -> queueAdd(value, steps);
            case QUEUE_REMOVE -> queueRemove(steps);
            case QUEUE_PEEK -> queuePeek(steps);
            case TREE_ADD -> treeAdd(value, steps);
            case TREE_REMOVE -> treeRemove(value, steps);
            case TREE_SEARCH -> treeSearch(value, steps);
            case TREE_INORDER -> treeInorder(steps);
            case SORT_BUBBLE -> bubbleSort(steps);
            case SORT_INSERTION -> insertionSort(steps);
            case SORT_SHUFFLE -> shuffle(steps);
        };
        return new Simulation(action, steps, Lessons.complexity(action), outcome);
    }

    private String vectorAdd(int value, List<Step> steps) {
        require(vectorSize < vector.length, "Vetor cheio: as " + vector.length + " posições estão ocupadas. Aumente a capacidade ou remova um elemento.");
        add(steps, StructureType.VECTOR, vectorSize, -1, "Localizar vaga", "A próxima posição livre é " + vectorSize + ".", 0);
        vector[vectorSize++] = value;
        add(steps, StructureType.VECTOR, vectorSize - 1, -1, "Valor inserido", value + " ocupa a posição " + (vectorSize - 1) + ".", 1);
        return "Inserção concluída na posição " + (vectorSize - 1) + ".";
    }

    private String vectorRemove(int position, List<Step> steps) {
        require(position >= 0 && position < vectorSize, "Posição inválida. Escolha um índice de 0 a " + (vectorSize - 1) + ".");
        int removed = vector[position];
        add(steps, StructureType.VECTOR, position, -1, "Selecionar posição", "O valor " + removed + " será removido. Os seguintes devem deslocar uma casa.", 0);
        for (int i = position; i < vectorSize - 1; i++) {
            vector[i] = vector[i + 1];
            add(steps, StructureType.VECTOR, i, i + 1, "Deslocar valor", "O valor da posição " + (i + 1) + " passa para " + i + ".", 1);
        }
        vectorSize--;
        add(steps, StructureType.VECTOR, -1, -1, "Remoção concluída", "O tamanho lógico diminui para " + vectorSize + ". A capacidade continua em " + vector.length + ".", 2);
        return "Valor " + removed + " removido.";
    }

    private String vectorUpdate(int position, int value, List<Step> steps) {
        require(position >= 0 && position < vectorSize, "Posição inválida. Escolha um índice de 0 a " + (vectorSize - 1) + ".");
        int old = vector[position];
        add(steps, StructureType.VECTOR, position, -1, "Acesso direto", "O índice permite chegar à posição sem percorrer o vetor.", 0);
        vector[position] = value;
        add(steps, StructureType.VECTOR, position, -1, "Atualização", old + " foi substituído por " + value + ".", 1);
        return "Posição " + position + " atualizada.";
    }

    private String linearSearch(StructureType type, int value, List<Step> steps) {
        List<Integer> elements = values(type);
        for (int i = 0; i < elements.size(); i++) {
            int current = elements.get(i);
            add(steps, type, i, -1, "Comparar", "Posição " + i + ": " + current + (current == value ? " = " : " ≠ ") + value + ".", 0);
            if (current == value) {
                add(steps, type, i, -1, "Encontrado", "A busca termina na posição " + i + ".", 1);
                return "Valor encontrado na posição " + i + ".";
            }
        }
        add(steps, type, -1, -1, "Fim da busca", "Todos os " + elements.size() + " elementos foram examinados; valor ausente.", 2);
        return "Valor não encontrado.";
    }

    private int listSize() { return values(StructureType.LINKED_LIST).size(); }

    private String listAddFirst(int value, List<Step> steps) {
        require(listSize() < MAX_NODES, "Limite visual de 20 nós atingido.");
        add(steps, StructureType.LINKED_LIST, 0, -1, "Criar nó", "Um novo nó recebe " + value + " e aponta para a cabeça atual.", 0);
        Node newHead = new Node(value);
        newHead.next = listHead;
        listHead = newHead;
        add(steps, StructureType.LINKED_LIST, 0, -1, "Atualizar cabeça", "A cabeça agora aponta para o novo nó.", 1);
        return "Valor inserido no início.";
    }

    private String listAddLast(int value, List<Step> steps) {
        require(listSize() < MAX_NODES, "Limite visual de 20 nós atingido.");
        if (listHead == null) return listAddFirst(value, steps);
        Node current = listHead;
        int index = 0;
        while (current.next != null) {
            add(steps, StructureType.LINKED_LIST, index, -1, "Seguir referência", "O nó " + index + " aponta para o seguinte.", 0);
            current = current.next;
            index++;
        }
        add(steps, StructureType.LINKED_LIST, index, -1, "Encontrar cauda", "A referência próximo do último nó é nula.", 1);
        current.next = new Node(value);
        add(steps, StructureType.LINKED_LIST, index + 1, -1, "Ligar novo nó", "A antiga cauda agora aponta para " + value + ".", 2);
        return "Valor inserido no fim.";
    }

    private String listAddAt(int position, int value, List<Step> steps) {
        int size = listSize();
        require(size < MAX_NODES, "Limite visual de 20 nós atingido.");
        require(position >= 0 && position <= size, "Posição inválida. Use um índice de 0 a " + size + ".");
        if (position == 0) return listAddFirst(value, steps);
        Node previous = listHead;
        for (int i = 0; i < position - 1; i++) {
            add(steps, StructureType.LINKED_LIST, i, -1, "Percorrer", "Avance até o nó anterior à posição desejada.", 0);
            previous = previous.next;
        }
        add(steps, StructureType.LINKED_LIST, position - 1, -1, "Guardar ligação", "O novo nó apontará para o antigo próximo.", 1);
        Node inserted = new Node(value);
        inserted.next = previous.next;
        previous.next = inserted;
        add(steps, StructureType.LINKED_LIST, position, -1, "Religar", "O nó anterior agora aponta para " + value + ".", 2);
        return "Valor inserido na posição " + position + ".";
    }

    private String listRemove(int value, List<Step> steps) {
        Node previous = null;
        Node current = listHead;
        int index = 0;
        while (current != null) {
            add(steps, StructureType.LINKED_LIST, index, -1, "Comparar", current.value + (current.value == value ? " = " : " ≠ ") + value + ".", 0);
            if (current.value == value) {
                if (previous == null) listHead = current.next;
                else previous.next = current.next;
                add(steps, StructureType.LINKED_LIST, index, -1, "Desvincular nó", "A referência contorna o nó removido; os demais não se deslocam.", 1);
                return "Primeira ocorrência de " + value + " removida.";
            }
            previous = current;
            current = current.next;
            index++;
        }
        add(steps, StructureType.LINKED_LIST, -1, -1, "Valor ausente", "A busca chegou à referência nula sem encontrar " + value + ".", 2);
        return "Valor não encontrado; lista preservada.";
    }

    private String listRemoveAt(int position, List<Step> steps) {
        require(position >= 0 && position < listSize(), "Escolha a posição de um nó existente.");
        Node previous = null, current = listHead;
        for (int i = 0; i < position; i++) {
            add(steps, StructureType.LINKED_LIST, i, -1, "Seguir referência", "Avance até a posição " + position + ".", 0);
            previous = current; current = current.next;
        }
        int removed = current.value;
        add(steps, StructureType.LINKED_LIST, position, -1, "Selecionar nó", "A posição " + position + " contém " + removed + ".", 1);
        if (previous == null) listHead = current.next;
        else previous.next = current.next;
        add(steps, StructureType.LINKED_LIST, -1, -1, "Religar referências", previous == null
                ? "A cabeça passa a apontar para o próximo nó."
                : "O nó anterior aponta para o sucessor do removido. Os valores dos outros nós são preservados.", 2);
        return "Nó na posição " + position + " removido (valor " + removed + ").";
    }

    private String stackPush(int value, List<Step> steps) {
        require(values(StructureType.STACK).size() < MAX_NODES, "Limite visual de 20 nós atingido.");
        add(steps, StructureType.STACK, 0, -1, "Criar elemento", "O novo elemento apontará para o topo atual.", 0);
        Node node = new Node(value);
        node.next = stackTop;
        stackTop = node;
        add(steps, StructureType.STACK, 0, -1, "Novo topo", value + " passa a ser o primeiro a sair.", 1);
        return value + " empilhado.";
    }

    private String stackPop(List<Step> steps) {
        require(stackTop != null, "Pilha vazia: não há elemento para desempilhar.");
        int removed = stackTop.value;
        add(steps, StructureType.STACK, 0, -1, "Ler topo", "O topo contém " + removed + ".", 0);
        stackTop = stackTop.next;
        add(steps, StructureType.STACK, 0, -1, "Avançar topo", "O antigo topo sai; o seguinte assume seu lugar.", 1);
        return removed + " desempilhado.";
    }

    private String stackPeek(List<Step> steps) {
        require(stackTop != null, "Pilha vazia: não há topo para consultar.");
        add(steps, StructureType.STACK, 0, -1, "Consultar", "O topo é " + stackTop.value + "; nada foi removido.", 0);
        return "Topo: " + stackTop.value + ".";
    }

    private String queueAdd(int value, List<Step> steps) {
        require(values(StructureType.QUEUE).size() < MAX_NODES, "Limite visual de 20 nós atingido.");
        add(steps, StructureType.QUEUE, -1, -1, "Criar elemento", "O novo elemento entra depois da cauda atual.", 0);
        Node node = new Node(value);
        if (queueTail == null) queueHead = node;
        else queueTail.next = node;
        queueTail = node;
        add(steps, StructureType.QUEUE, values(StructureType.QUEUE).size() - 1, -1, "Atualizar cauda", value + " aguardará a sua vez no fim da fila.", 1);
        return value + " enfileirado.";
    }

    private String queueRemove(List<Step> steps) {
        require(queueHead != null, "Fila vazia: não há elemento para desenfileirar.");
        int removed = queueHead.value;
        add(steps, StructureType.QUEUE, 0, -1, "Ler início", removed + " foi o primeiro a entrar e será o primeiro a sair.", 0);
        queueHead = queueHead.next;
        if (queueHead == null) queueTail = null;
        add(steps, StructureType.QUEUE, 0, -1, "Avançar início", "O próximo elemento passa a ser o início da fila.", 1);
        return removed + " desenfileirado.";
    }

    private String queuePeek(List<Step> steps) {
        require(queueHead != null, "Fila vazia: não há início para consultar.");
        add(steps, StructureType.QUEUE, 0, -1, "Consultar", "O início é " + queueHead.value + "; a fila não mudou.", 0);
        return "Início: " + queueHead.value + ".";
    }

    private String treeAdd(int value, List<Step> steps) {
        if (treeRoot == null) {
            require(values(StructureType.BST).size() < MAX_NODES, "Limite visual de 20 nós atingido.");
            treeRoot = new Tree(value);
            add(steps, StructureType.BST, value, -1, "Criar raiz", "A árvore estava vazia; " + value + " torna-se a raiz.", 0);
            return value + " inserido como raiz.";
        }
        Tree current = treeRoot;
        while (true) {
            add(steps, StructureType.BST, current.value, -1, "Comparar", value + (value < current.value ? " < " : value > current.value ? " > " : " = ") + current.value + ".", 0);
            if (value == current.value) {
                add(steps, StructureType.BST, current.value, -1, "Duplicado", "Esta árvore não armazena valores repetidos.", 1);
                return "Valor já presente; árvore preservada.";
            }
            if (value < current.value) {
                if (current.left == null) {
                    require(values(StructureType.BST).size() < MAX_NODES, "Limite visual de 20 nós atingido.");
                    current.left = new Tree(value);
                    add(steps, StructureType.BST, value, -1, "Inserir à esquerda", value + " é menor que o pai " + current.value + ".", 2);
                    return value + " inserido.";
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    require(values(StructureType.BST).size() < MAX_NODES, "Limite visual de 20 nós atingido.");
                    current.right = new Tree(value);
                    add(steps, StructureType.BST, value, -1, "Inserir à direita", value + " é maior que o pai " + current.value + ".", 2);
                    return value + " inserido.";
                }
                current = current.right;
            }
        }
    }

    private String treeSearch(int value, List<Step> steps) {
        Tree current = treeRoot;
        while (current != null) {
            add(steps, StructureType.BST, current.value, -1, "Comparar", value + (value < current.value ? " < " : value > current.value ? " > " : " = ") + current.value + ".", 0);
            if (value == current.value) {
                add(steps, StructureType.BST, value, -1, "Encontrado", "A comparação localizou o valor.", 1);
                return "Valor encontrado.";
            }
            current = value < current.value ? current.left : current.right;
        }
        add(steps, StructureType.BST, NO_TREE_FOCUS, -1, "Caminho encerrado", "A referência é nula; o valor não está na árvore.", 2);
        return "Valor não encontrado.";
    }

    private String treeRemove(int value, List<Step> steps) {
        Tree current = treeRoot;
        while (current != null && current.value != value) {
            add(steps, StructureType.BST, current.value, -1, "Procurar nó", value + (value < current.value ? " é menor: esquerda." : " é maior: direita."), 0);
            current = value < current.value ? current.left : current.right;
        }
        if (current == null) {
            add(steps, StructureType.BST, NO_TREE_FOCUS, -1, "Valor ausente", "Nenhum nó corresponde a " + value + ".", 0);
            return "Valor não encontrado; árvore preservada.";
        }
        add(steps, StructureType.BST, value, -1, "Nó localizado", "A remoção deve preservar a regra da árvore de busca.", 1);
        String caseName = current.left == null && current.right == null ? "folha" :
                current.left == null || current.right == null ? "um filho" : "dois filhos";
        treeRoot = delete(treeRoot, value);
        add(steps, StructureType.BST, NO_TREE_FOCUS, -1, "Árvore reorganizada", "Caso de " + caseName + ": a propriedade de busca foi preservada.", 2);
        return value + " removido (caso de " + caseName + ").";
    }

    private static Tree delete(Tree node, int value) {
        if (value < node.value) node.left = delete(node.left, value);
        else if (value > node.value) node.right = delete(node.right, value);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            Tree successor = node.right;
            while (successor.left != null) successor = successor.left;
            node.value = successor.value;
            node.right = delete(node.right, successor.value);
        }
        return node;
    }

    private String treeInorder(List<Step> steps) {
        List<Integer> visited = new ArrayList<>();
        treeWalk(treeRoot, visited, steps);
        add(steps, StructureType.BST, NO_TREE_FOCUS, -1, "Percurso concluído", "Esquerda → nó → direita produz valores em ordem crescente.", 2);
        return "Em ordem: " + visited + ".";
    }

    private void treeWalk(Tree node, List<Integer> visited, List<Step> steps) {
        if (node == null) return;
        treeWalk(node.left, visited, steps);
        visited.add(node.value);
        add(steps, StructureType.BST, node.value, -1, "Visitar nó", "Visitados até agora: " + visited + ".", 1);
        treeWalk(node.right, visited, steps);
    }

    private String bubbleSort(List<Step> steps) {
        int comparisons = 0;
        int swaps = 0;
        for (int end = sorting.length - 1; end > 0; end--) {
            boolean changed = false;
            for (int i = 0; i < end; i++) {
                comparisons++;
                add(steps, StructureType.SORTING, i, i + 1, "Comparar vizinhos", sorting[i] + " e " + sorting[i + 1] + ": " + (sorting[i] > sorting[i + 1] ? "trocar" : "manter") + ".", 0);
                if (sorting[i] > sorting[i + 1]) {
                    int temp = sorting[i]; sorting[i] = sorting[i + 1]; sorting[i + 1] = temp;
                    swaps++;
                    changed = true;
                    add(steps, StructureType.SORTING, i, i + 1, "Troca", "O maior dos dois avançou uma posição.", 1);
                }
            }
            if (!changed) break;
        }
        add(steps, StructureType.SORTING, -1, -1, "Ordenação concluída", comparisons + " comparações e " + swaps + " trocas.", 2);
        return "Bolha: " + comparisons + " comparações, " + swaps + " trocas.";
    }

    private String insertionSort(List<Step> steps) {
        int comparisons = 0;
        int shifts = 0;
        for (int i = 1; i < sorting.length; i++) {
            int key = sorting[i];
            int j = i - 1;
            add(steps, StructureType.SORTING, i, -1, "Escolher chave", "Insira " + key + " na parte já ordenada, à esquerda.", 0);
            while (j >= 0) {
                comparisons++;
                add(steps, StructureType.SORTING, j, j + 1, "Comparar", sorting[j] + (sorting[j] > key ? " > " : " ≤ ") + key + ".", 1);
                if (sorting[j] <= key) break;
                sorting[j + 1] = sorting[j];
                shifts++;
                add(steps, StructureType.SORTING, j, j + 1, "Abrir espaço", sorting[j] + " move uma casa à direita.", 2);
                j--;
            }
            sorting[j + 1] = key;
            add(steps, StructureType.SORTING, j + 1, -1, "Inserir chave", key + " ocupa a posição correta na parte ordenada.", 3);
        }
        add(steps, StructureType.SORTING, -1, -1, "Ordenação concluída", comparisons + " comparações e " + shifts + " deslocamentos.", 4);
        return "Inserção: " + comparisons + " comparações, " + shifts + " deslocamentos.";
    }

    private String shuffle(List<Step> steps) {
        for (int i = sorting.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = sorting[i]; sorting[i] = sorting[j]; sorting[j] = temp;
        }
        add(steps, StructureType.SORTING, -1, -1, "Novo exemplo", "Os mesmos valores foram embaralhados para uma nova tentativa.", 0);
        return "Exemplo embaralhado.";
    }

    public void reset(StructureType type) {
        switch (type) {
            case VECTOR -> vectorSize = 0;
            case LINKED_LIST -> listHead = null;
            case STACK -> stackTop = null;
            case QUEUE -> { queueHead = null; queueTail = null; }
            case BST -> treeRoot = null;
            case SORTING -> sorting = new int[] { 7, 3, 9, 2, 6, 1, 8, 4 };
        }
    }

    public void load(StructureType type, List<Integer> input) {
        int max = type == StructureType.VECTOR ? vector.length : MAX_NODES;
        require(input.size() <= max, "Este módulo aceita no máximo " + max + " valores.");
        for (int value : input) require(value >= -999 && value <= 999, "Valores fora do intervalo -999 a 999.");
        if (type == StructureType.BST) require(input.stream().distinct().count() == input.size(), "A árvore não aceita valores repetidos.");
        reset(type);
        if (type == StructureType.SORTING) {
            sorting = input.stream().mapToInt(Integer::intValue).toArray();
            return;
        }
        for (int value : input) switch (type) {
            case VECTOR -> vector[vectorSize++] = value;
            case LINKED_LIST -> {
                Node node = new Node(value);
                if (listHead == null) listHead = node;
                else { Node tail = listHead; while (tail.next != null) tail = tail.next; tail.next = node; }
            }
            case STACK -> { Node node = new Node(value); if (stackTop == null) stackTop = node;
                else { Node tail = stackTop; while (tail.next != null) tail = tail.next; tail.next = node; } }
            case QUEUE -> { Node node = new Node(value); if (queueTail == null) queueHead = node; else queueTail.next = node; queueTail = node; }
            case BST -> { if (treeRoot == null) treeRoot = new Tree(value);
                else { Tree cursor = treeRoot; while (true) {
                    if (value < cursor.value) { if (cursor.left == null) { cursor.left = new Tree(value); break; } cursor = cursor.left; }
                    else { if (cursor.right == null) { cursor.right = new Tree(value); break; } cursor = cursor.right; }
                } } }
            case SORTING -> { }
        };
    }

    public void example(StructureType type) {
        load(type, switch (type) {
            case VECTOR -> List.of(12, 7, 24, 5);
            case LINKED_LIST -> List.of(14, 8, 22);
            case STACK -> List.of(9, 6, 3);
            case QUEUE -> List.of(4, 11, 18);
            case BST -> List.of(8, 3, 10, 1, 6, 14, 4, 7, 13);
            case SORTING -> List.of(7, 3, 9, 2, 6, 1, 8, 4);
        });
    }
}
