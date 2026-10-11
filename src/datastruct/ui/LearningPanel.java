package datastruct.ui;

import datastruct.core.StructureType;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class LearningPanel extends JPanel {
    private final JLabel title = Theme.label("", 29, true);
    private final JEditorPane article = new JEditorPane("text/html", "");
    private final JComboBox<StructureType> selector = new JComboBox<>(StructureType.values());
    private java.util.function.Consumer<StructureType> onSelect = type -> { };
    private boolean updating;
    public void onSelect(java.util.function.Consumer<StructureType> listener) { onSelect = listener; }
    public LearningPanel(Runnable explore) {
        super(new BorderLayout(0, 20)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 30, 24, 30));
        JPanel heading = Theme.panel(new BorderLayout()); heading.add(title);
        JButton button = Theme.button("Experimentar", true); button.addActionListener(event -> explore.run());
        JPanel controls = Theme.panel(new FlowLayout(FlowLayout.RIGHT,10,0)); selector.setPreferredSize(new Dimension(175,36));
        selector.setName("learningStructure"); selector.addActionListener(event -> { if (!updating) onSelect.accept((StructureType)selector.getSelectedItem()); });
        controls.add(selector); controls.add(button); heading.add(controls,BorderLayout.EAST);
        add(heading, BorderLayout.NORTH); article.setEditable(false); article.setBackground(Color.WHITE);
        article.setBorder(new EmptyBorder(12, 20, 18, 20));
        JScrollPane scroll = new JScrollPane(article); scroll.setBorder(new Theme.RoundBorder(Theme.LINE, 16, new Insets(1,1,1,1))); add(scroll);
    }
    public void show(StructureType type) {
        title.setText(type.title);
        updating = true; selector.setSelectedItem(type); updating = false;
        String content = switch(type) {
            case VECTOR -> section("Como funciona", "Posições contíguas guardam valores acessados diretamente por índice. O primeiro índice é 0. Capacidade é o número de vagas reservadas; tamanho é a quantidade de valores inseridos.")
                + section("Regra essencial", "Neste laboratório, o trecho ocupado vai de 0 até tamanho − 1. Inserir ao fim usa a próxima vaga. Remover uma posição desloca os valores seguintes para manter esse trecho contínuo. Redimensionar cria um novo arranjo e copia os valores.")
                + costs("Acessar / atualizar por índice|O(1);Inserir ao fim, com vaga|O(1);Buscar / remover / redimensionar|O(n)")
                + section("Experimente", "Crie um vetor de capacidade 5. Insira 12, 7, 24. Remova o índice 1 e acompanhe o deslocamento de 24. Aumente a capacidade para 8 e compare tamanho e capacidade.")
                + section("Atenção", "Índice e valor são diferentes: o valor 24 pode estar no índice 1. Uma vaga livre não é um zero armazenado. Mover um bloco no desenho não muda seu índice.");
            case LINKED_LIST -> section("Como funciona", "Cada nó possui um valor e uma referência para o próximo. A cabeça identifica o primeiro nó, e a referência do último é null. Os nós não precisam ocupar posições contíguas na memória.")
                + section("Regra essencial", "Inserir ou remover altera referências. Os valores dos outros nós são preservados. Para acessar uma posição, é necessário percorrer a cadeia a partir da cabeça.")
                + costs("Inserir no início|O(1);Buscar / acessar por posição|O(n);Inserir no fim / por posição|O(n);Remover por valor / posição|O(n)")
                + section("Experimente", "Insira 14, 8, 22 no fim. Adicione 5 no início. Remova o nó da posição 2 e observe a ligação entre seu anterior e seu sucessor.")
                + section("Atenção", "Valores repetidos podem pertencer a nós diferentes. Remover por valor escolhe a primeira ocorrência; remover por posição identifica o nó exato. Esta implementação não mantém uma referência de cauda para a lista.");
            case STACK -> section("Como funciona", "Uma pilha segue LIFO: o último elemento a entrar é o primeiro a sair. Todas as inserções e remoções acontecem no topo.")
                + section("Regra essencial", "Empilhar cria um novo topo ligado ao anterior. Desempilhar retira o topo e faz o elemento seguinte assumir seu lugar. Consultar o topo não remove nada.")
                + costs("Empilhar|O(1);Desempilhar|O(1);Consultar topo|O(1)")
                + section("Experimente", "Empilhe 3, 6, 9 nessa ordem. Desempilhe duas vezes e observe a saída de 9 e depois 6. Use desfazer para recuperar o estado anterior.")
                + section("Onde aparece", "Chamadas de funções, histórico de desfazer e verificação de parênteses. A edição direta prepara um conjunto; as operações de pilha continuam restritas ao topo.");
            case QUEUE -> section("Como funciona", "Uma fila segue FIFO: o primeiro elemento a entrar é o primeiro a sair. O início aponta para quem será atendido; a cauda aponta para o último elemento.")
                + section("Regra essencial", "Enfileirar acrescenta um nó na cauda. Desenfileirar remove o início. Ao remover o último elemento, tanto início quanto cauda precisam tornar-se null.")
                + costs("Enfileirar|O(1);Desenfileirar|O(1);Consultar início|O(1)")
                + section("Experimente", "Enfileire 4, 11, 18. Retire um elemento e confirme que 4 sai primeiro. Esvazie a fila e insira um novo valor para observar as duas referências.")
                + section("Onde aparece", "Filas de impressão, atendimento e escalonamento de tarefas. Esta fila utiliza nós encadeados e mantém referências de início e cauda.");
            case BST -> section("Como funciona", "Na árvore binária de busca, cada nó tem até dois filhos. Os valores da subárvore esquerda são menores; os da direita são maiores. Esta implementação não permite valores repetidos.")
                + section("Regra essencial", "Buscar e inserir escolhem um ramo a cada comparação. Remover uma folha elimina o nó; com um filho, religa esse filho; com dois, substitui pelo sucessor em ordem, o menor valor da subárvore direita.")
                + costs("Buscar / inserir / remover|O(h);Percorrer em ordem|O(n);Pior caso, árvore em cadeia|h = n − 1")
                + section("Experimente", "Insira 8, 3, 10, 1, 6. Depois compare com a inserção de 1, 3, 6, 8, 10 em uma árvore vazia. A ordem de inserção muda a altura. Remova uma raiz com dois filhos.")
                + section("Atenção", "Esta árvore não se equilibra automaticamente. O(log n) para busca depende de altura logarítmica; no pior caso o custo é O(n). Arrastar um nó não muda sua relação de pai e filho.");
            case SORTING -> section("Como funciona", "A ordenação reorganiza os valores em ordem crescente. No método bolha, pares vizinhos fora de ordem são trocados. Na inserção, cada chave é posicionada dentro da parte já ordenada.")
                + section("Regra essencial", "A bolha empurra valores maiores para o fim. A inserção guarda a chave antes de deslocar elementos maiores, para não perdê-la. Barras negativas aparecem abaixo da linha zero.")
                + costs("Bolha / inserção: pior caso|O(n²);Melhor caso, já ordenado|O(n);Espaço auxiliar dos algoritmos|O(1)")
                + section("Experimente", "Use 7, 3, 7, −2, 0. Execute um algoritmo, anote comparações e trocas ou deslocamentos, desfaça e execute o outro sobre o mesmo conjunto inicial.")
                + section("Atenção", "Os dois métodos preservam a ordem relativa de valores iguais nesta implementação. Trocas e deslocamentos são operações diferentes; compare as métricas com esse cuidado.");
        };
        article.setText("<html><body style='font-family:Segoe UI;font-size:13px;color:#2b2440;margin:8px'>" + content
                + "<p style='color:#79738b;font-size:10px'>n = quantidade de elementos; h = altura da árvore. Custos dos algoritmos, sem renderização, validação e histórico.</p></body></html>");
        article.setCaretPosition(0);
    }
    private static String section(String heading, String text) { return "<h3 style='color:#7d4bc5;margin-top:20px;margin-bottom:6px'>" + heading + "</h3><p style='margin-top:0'>" + text + "</p>"; }
    private static String costs(String rows) {
        StringBuilder table = new StringBuilder("<h3 style='color:#7d4bc5'>Custo das operações</h3><table width='100%' cellpadding='8' cellspacing='1' bgcolor='#eee8f6'>");
        for (String row : rows.split(";")) { String[] cells = row.split("\\|"); table.append("<tr bgcolor='#faf8fe'><td>").append(cells[0]).append("</td><td><b>").append(cells[1]).append("</b></td></tr>"); }
        return table.append("</table>").toString();
    }
}
