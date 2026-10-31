# Requisitos

## 1. Requisitos Funcionais (RF)

### RF01 – Gestão da Partida e Jogadores

- **RF01.1 – Configuração da Partida:** O sistema deve permitir a criação de partidas com 2 a 5 participantes, suportando jogadores exclusivamente humanos, inteiramente controlados por Inteligência Artificial ou mistos (Humanos e IA).
- **RF01.2 – Ordem de Turnos:** O sistema deve controlar a vez de cada jogador de forma cíclica e sequencial até o término do jogo.
- **RF01.3 – Gestão de Reserva de Meeples:** Cada jogador deve iniciar a partida com uma reserva de 7 Meeples (seguidores) para uso no tabuleiro.
- **RF01.4 – Marcador de pontos:** O sistema deve mostrar a pontuação em tempo real de cada jogador em formato numérico.
- **RF01.5 – Encerramento da Partida:** A partida deve ser encerrada automaticamente quando o Baralho / Pilha de Peças estiver totalmente vazio e o último jogador não puder mais realizar jogadas válidas.

### RF02 – Gestão do Baralho e Peças

- **RF02.1 – Inicialização e Embaralhamento:** O sistema deve criar um baralho contendo as 72 peças (azulejos) do jogo base devidamente embaralhadas.
- **RF02.2 – Compra de Peças:** A cada turno, o jogador da vez compra a peça do topo do baralho.
- **RF02.3 – Descarte de Peças Sem Encaixe:** Se a peça comprada não possuir nenhum posicionamento/orientação válido em todo o tabuleiro atual, ela deve ser descartada do jogo e uma nova peça deve ser automaticamente comprada pelo jogador.
- **RF02.4 – Rotação da Peça:** O jogador deve poder rotacionar a peça comprada em ângulos de 90° (para a direita/esquerda) antes de tentar o posicionamento.

### RF03 – Tabuleiro e Posicionamento de Peças

- **RF03.1 – Peça Inicial:** O tabuleiro deve ser inicializado com a peça de início padrão no centro da grade (coordenada x=0, y=0).
- **RF03.2 – Mapeamento do Tabuleiro:** O sistema deve mapear o tabuleiro por coordenadas bidimensionais (x, y), onde a chave indexa a posição e o valor guarda a peça e sua rotação.
- **RF03.3 – Validação de Encaixe Geométrico:** O sistema deve verificar se a posição selecionada possui espaço livre e é adjacente a pelo menos uma peça já colocada.
- **RF03.4 – Validação de Borda Compatível:** O sistema deve garantir que todos os lados conectados (Norte, Sul, Leste, Oeste) coincidam perfeitamente com os tipos de terrenos das peças vizinhas (ex.: Estrada com Estrada, Cidade com Cidade, Campo com Campo).
- **RF03.5 – Fusão de Regiões Conexas:** O sistema deve unificar regiões isoladas do mesmo tipo que forem conectadas por uma nova peça, recalculando a maioria de Meeples.

### RF04 – Colocação e Regras de Meeples (Seguidores)

- **RF04.1 – Alocação do Meeple:** Após colocar uma peça válida, o jogador pode opcionalmente alocar um Meeple de sua reserva em um dos Segmentos (Cidade, Estrada, Mosteiro, Campo) contidos apenas na peça recém-colocada.
- **RF04.2 – Papel do Meeple:** O papel do Meeple (Ladrão na estrada, Cavaleiro na cidade, Monge no mosteiro, Fazendeiro no campo) deve ser determinado dinamicamente com base no tipo de segmento onde foi alocado.
  - **RF04.2.1 – Papel de Ladrão:** O sistema deve atribuir dinamicamente o papel de Ladrão ao Meeple quando este for alocado em um segmento do tipo Estrada.
  - **RF04.2.2 – Papel de Cavaleiro:** O sistema deve atribuir dinamicamente o papel de Cavaleiro ao Meeple quando este for alocado em um segmento do tipo Cidade.
  - **RF04.2.3 – Papel de Monge:** O sistema deve atribuir dinamicamente o papel de Monge ao Meeple quando este for alocado em um segmento do tipo Mosteiro.
  - **RF04.2.4 – Papel de Fazendeiro:** O sistema deve atribuir dinamicamente o papel de Fazendeiro ao Meeple quando este for alocado em um segmento do tipo Campo.
- **RF04.3 – Restrição de Região Ocupada:** O sistema não deve permitir que um Meeple seja colocado em um segmento cuja região conectada (definida como o componente conexo de segmentos do mesmo tipo interligados por uma ou mais peças adjacentes no tabuleiro) já possua um ou mais Meeples ativos de qualquer jogador.
- **RF04.4 – Devolução à Reserva:** Ao concluir uma região (Estrada, Cidade ou Mosteiro), os Meeples presentes nela devem pontuar (receber sua respectiva pontuação) e retornar imediatamente à reserva dos seus respectivos donos.
- **RF04.5 – Permanência do Fazendeiro:** Meeples posicionados em Campos (Fazendeiros) devem permanecer no tabuleiro até o fim da partida, sem retornar à reserva.

### RF05 – Pontuação e Conclusão de Regiões

- **RF05.1 – Verificação de Conclusão (Durante o Jogo):** Ao final do posicionamento da peça, o sistema deve verificar se alguma região foi concluída:
  - **Estrada:** Fechada em ambas as extremidades (cruzamentos, cidades, mosteiros ou laços completos).
  - **Cidade:** Com todas as muralhas fechadas e sem lacunas internas.
  - **Mosteiro:** Completamente cercado por 8 peças vizinhas.
- **RF05.2 – Regra de Maioria, Empate e Conclusão:** Ao concluir uma região durante a partida ou ao realizar a contagem final no encerramento do jogo, a pontuação total é atribuída ao jogador que possuir o maior número de Meeples naquela região. Em caso de empate no número máximo de Meeples, todos os jogadores empatados recebem a pontuação integral.
- **RF05.3 – Cálculo de Pontos Durante a Partida:**
  - **Estrada Concluída:** 1 ponto por peça que compõe a estrada.
  - **Cidade Concluída:** 2 pontos por peça + 2 pontos extras por cada Brasão existente na cidade.
  - **Mosteiro Concluído:** 9 pontos (1 da peça do mosteiro + 8 das peças ao redor).
- **RF05.4 – Contagem Final de Pontos (Fim do Jogo):**
  - **Estradas Incompletas:** 1 ponto por peça.
  - **Cidades Incompletas:** 1 ponto por peça + 1 ponto por brasão.
  - **Mosteiros Incompletos:** 1 ponto pela peça do mosteiro + 1 ponto por cada peça vizinha.
  - **Campos (Fazendas):** 3 pontos para cada Cidade Concluída adjacente ao campo contínuo.

### RF06 – Inteligência Artificial (IA)

- **RF06.1 – Autonomia do Ciclo de Turno:** O sistema deve permitir que o agente de IA execute todas as fases do seu turno de maneira independente, sem qualquer necessidade de interação humana. Esse ciclo abrange a leitura do estado atual do tabuleiro, a avaliação das opções de encaixe da peça comprada, a escolha da rotação ideal, a decisão estratégica sobre a alocação de um Meeple e a passagem da vez.
- **RF06.2 – Avaliação Estratégica de Jogadas:** O motor de decisão da IA deve priorizar jogadas que resultem em:
  - Conclusão imediata de regiões próprias (cidades, estradas ou mosteiros), garantindo a pontuação e a recuperação rápida de Meeples.
  - Obstrução do avanço dos oponentes, posicionando peças que dificultem ou impossibilitem o fechamento de grandes áreas adversárias.
  - Inserção calculada em áreas vizinhas a regiões altamente pontuadas que estejam em disputa, visando a divisão ou conquista dos pontos no fechamento.
- **RF06.3 – Gestão de Recursos (Meeples):** A decisão de alocar um seguidor deve levar em conta o estágio do jogo e o saldo disponível: adotando uma postura expansionista e de longo prazo quando a reserva estiver confortável, e uma postura conservadora — priorizando regiões prestes a serem fechadas — quando restarem poucos seguidores.
- **RF06.4** – Caso a peça comprada não possua posicionamento válido, o sistema deve processar o descarte e o novo saque para a IA automaticamente, aplicando a regra padrão do jogo.

## 2. Requisitos Não Funcionais (RNF)

### RNF01 – Arquitetura e Modelagem

- **RNF01.1 – Separação em Camadas (MVC):** O sistema deve ser estruturado em camadas claras (Pacotes Modelo, Visao e Controle), garantindo que dados das regras de negócio (como a classe `Peca`) não dependam diretamente de atributos visuais/GUI (ex.: sem dependência direta de imagens dentro do domínio puro).
- **RNF01.2 – Abstração de Regiões:** O modelo conceitual de domínio deve utilizar a hierarquia de Peça -> Segmento -> Região para permitir o correto agrupamento de áreas conectadas em rede/grafo e validação das regras de vizinhança e bloqueio.
- **RNF01.3 – Polimorfismo no Cálculo de Regiões:** A classe abstrata `Regiao` deve conter subclasses especializadas (`Cidade`, `Estrada`, `Mosteiro`, `Campo`) implementando comportamentos polimórficos para `estaCompleta()` e `calcularPontos()`.

### RNF02 – Desempenho e Estrutura de Dados

- **RNF02.1 – Indexação Eficiente do Tabuleiro:** A estrutura do tabuleiro deve utilizar um mapa Hash (`HashMap` ou `Map<Posicao, Peca>`) para garantir tempo de busca eficiente O(1) ao consultar coordenadas (x, y) e permitir a expansão do tabuleiro sem limite para qualquer direção.
- **RNF02.2 – Desempenho dos Algoritmos de Grafos:** A verificação do fechamento de regiões e conectividade dos campos deve ser executada de forma imperceptível ao usuário (tempo de resposta < 100ms).

### RNF03 – Qualidade e Padrões de Código

- **RNF03.1 – Nomenclatura e Notação UML:** O código e os diagramas do sistema devem seguir convenções estritas de OO e notação UML (uso correto de classes abstratas em vez de estereótipos incorretos como `<<primitive>>`, cardinalidades/multiplicidades declaradas, tipos em todos os atributos e assinaturas completas de métodos com parâmetros e retornos).

### RNF04 – Usabilidade e Interface

- **RNF04.1 – Destaque de Posições Válidas:** A interface deve indicar visualmente ao jogador onde a peça atual pode ser encaixada.
- **RNF04.2 – Histórico/Placar Visível:** O placar geral de pontuação e o número de Meeples disponíveis na reserva de cada jogador devem ser mantidos visíveis na interface durante todo o decorrer da partida.
- **RNF04.3 – Rastreador do Baralho:** A interface deve exibir durante todo o jogo a contagem exata de peças que ainda restam na pilha de compras, auxiliando os jogadores humanos em tomadas de decisão.

### RNF05 – Inteligência Artificial

- **RNF05.1 – Desempenho de Decisão:** O processamento completo do algoritmo da IA deve ser executado em um tempo máximo de 2 segundos.
- **RNF05.2 – Desacoplamento Visual (Isolamento Arquitetural):** A lógica de tomada de decisão da IA deve residir estritamente na camada de Controle. O agente autônomo deve basear suas escolhas puramente na leitura estrutural do Modelo (matrizes, grafos e coordenadas do tabuleiro), sem possuir qualquer acoplamento, dependência ou acesso direto aos componentes da interface gráfica (Visão).
