package datastruct.core;

public enum StructureType {
    VECTOR("Vetor", "Acesso por índice", "O vetor reserva posições contíguas. Você define a capacidade; o tamanho indica quantas posições estão ocupadas. A contagem dos índices começa em zero.", "Estático • acesso O(1)"),
    LINKED_LIST("Lista encadeada", "Nós e referências", "Cada nó guarda um valor e a referência para o próximo. Inserções mudam ligações, sem deslocar os demais valores.", "Dinâmica • busca O(n)"),
    STACK("Pilha", "Último a entrar, primeiro a sair", "Toda inserção e remoção ocorre no topo. Observe como o último valor colocado é o primeiro retirado.", "LIFO • topo O(1)"),
    QUEUE("Fila", "Primeiro a entrar, primeiro a sair", "Um valor entra no fim e sai pelo início. É a ordem de atendimento de uma fila comum.", "FIFO • extremidades O(1)"),
    BST("Árvore de busca", "Decisões à esquerda e à direita", "Valores menores seguem à esquerda; maiores, à direita. A forma da árvore depende da ordem de inserção.", "Busca O(h) • h é a altura"),
    SORTING("Ordenação", "Acompanhe comparações e trocas", "Compare dois algoritmos de ordenação sobre o mesmo conjunto. Cada quadro mostra uma decisão do algoritmo.", "Bolha e inserção • O(n²)");

    public final String title;
    public final String subtitle;
    public final String lesson;
    public final String complexity;
    @Override public String toString() { return title; }

    StructureType(String title, String subtitle, String lesson, String complexity) {
        this.title = title;
        this.subtitle = subtitle;
        this.lesson = lesson;
        this.complexity = complexity;
    }
}
