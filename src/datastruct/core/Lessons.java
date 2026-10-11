package datastruct.core;

public final class Lessons {
    private Lessons() { }

    public static String complexity(Action action) {
        return switch (action) {
            case VECTOR_ADD, VECTOR_UPDATE, STACK_PUSH, STACK_POP, STACK_PEEK,
                 QUEUE_ADD, QUEUE_REMOVE, QUEUE_PEEK, LIST_ADD_FIRST -> "Tempo O(1)";
            case VECTOR_REMOVE, VECTOR_SEARCH, LIST_ADD_LAST, LIST_ADD_AT,
                 LIST_REMOVE, LIST_REMOVE_AT, LIST_SEARCH -> "Tempo O(n)";
            case TREE_ADD, TREE_REMOVE, TREE_SEARCH -> "Tempo O(h), onde h é a altura; pior caso O(n)";
            case TREE_INORDER -> "Tempo O(n)";
            case SORT_BUBBLE, SORT_INSERTION -> "Tempo O(n²) no pior caso";
            case SORT_SHUFFLE -> "Tempo O(n)";
        };
    }

    public static String[] code(Action action) {
        return switch (action) {
            case VECTOR_ADD -> new String[] {
                "i ← tamanho", "vetor[i] ← valor; tamanho ← tamanho + 1" };
            case VECTOR_REMOVE -> new String[] {
                "removido ← vetor[posição]", "para i de posição até tamanho - 2: vetor[i] ← vetor[i + 1]",
                "tamanho ← tamanho - 1" };
            case VECTOR_SEARCH, LIST_SEARCH -> new String[] {
                "para cada elemento: compare com valor", "se igual: retorne a posição", "retorne ausente" };
            case VECTOR_UPDATE -> new String[] { "acesse vetor[posição]", "vetor[posição] ← valor" };
            case LIST_ADD_FIRST -> new String[] { "novo.próximo ← cabeça", "cabeça ← novo" };
            case LIST_ADD_LAST -> new String[] { "percorra até o último nó", "identifique a cauda", "cauda.próximo ← novo" };
            case LIST_ADD_AT -> new String[] { "percorra até posição - 1", "novo.próximo ← anterior.próximo",
                "anterior.próximo ← novo" };
            case LIST_REMOVE -> new String[] { "percorra comparando cada nó", "anterior.próximo ← atual.próximo",
                "se não encontrar: preserve a lista" };
            case LIST_REMOVE_AT -> new String[] { "percorra até a posição desejada",
                "guarde o nó a remover", "se posição = 0: cabeça ← atual.próximo; senão: anterior.próximo ← atual.próximo" };
            case STACK_PUSH -> new String[] { "novo.próximo ← topo", "topo ← novo" };
            case STACK_POP -> new String[] { "removido ← topo", "topo ← topo.próximo" };
            case STACK_PEEK -> new String[] { "retorne topo.valor sem remover" };
            case QUEUE_ADD -> new String[] { "novo entra depois da cauda", "cauda.próximo ← novo; cauda ← novo" };
            case QUEUE_REMOVE -> new String[] { "removido ← início", "início ← início.próximo; ajuste cauda se vazia" };
            case QUEUE_PEEK -> new String[] { "retorne início.valor sem remover" };
            case TREE_ADD -> new String[] { "compare valor com nó", "se igual: não inserir duplicado",
                "se menor: esquerda; se maior: direita; crie no vazio" };
            case TREE_REMOVE -> new String[] { "busque seguindo as comparações", "identifique folha, um filho ou dois filhos",
                "religue; com dois filhos, use o sucessor em ordem" };
            case TREE_SEARCH -> new String[] { "compare valor com nó e escolha o ramo", "se igual: encontrado",
                "se chegar a nulo: ausente" };
            case TREE_INORDER -> new String[] { "percorra subárvore esquerda", "visite nó",
                "percorra subárvore direita" };
            case SORT_BUBBLE -> new String[] { "compare cada par vizinho até o limite",
                "se fora de ordem: troque", "repita enquanto houver trocas" };
            case SORT_INSERTION -> new String[] { "chave ← vetor[i]", "compare com a parte ordenada",
                "desloque valores maiores à direita", "insira chave no espaço aberto",
                "repita para cada chave" };
            case SORT_SHUFFLE -> new String[] { "embaralhe os valores com Fisher-Yates" };
        };
    }

}
