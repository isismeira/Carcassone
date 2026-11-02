package carcassonne.controle;

import carcassonne.modelo.Baralho;
import carcassonne.modelo.Cor;
import carcassonne.modelo.EstrategiaHumana;
import carcassonne.modelo.EstrategiaIA;
import carcassonne.modelo.Jogada;
import carcassonne.modelo.Jogador;
import carcassonne.modelo.Partida;
import carcassonne.modelo.Peca;
import carcassonne.modelo.Posicao;
import carcassonne.modelo.Regiao;
import carcassonne.modelo.Segmento;
import carcassonne.modelo.Tabuleiro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orquestra a partida para a camada de Visao. Toda decisao de regra mora aqui
 * ou no modelo; nada nesta classe conhece HTTP, JSON ou imagens (RNF05.2).
 */
public class ControladorDePartida {
    /** Partida so de IAs: a tela pede um turno por vez para o jogo ser assistido. */
    public static final String MODO_ENTRE_IAS = "ai-vs-ai";

    private Partida partida;
    private Peca pecaAtual;
    private Jogada jogadaPendente;
    private String mensagem = "";
    private String modo = "ai";
    private boolean executandoTurnosIA;

    /**
     * Comeca uma partida nova: cria o jogador, embaralha, abre o tabuleiro com a
     * cartela inicial e ja compra a primeira cartela jogavel.
     */
    public synchronized void novaPartida(String nome, String modo) {
        String nomeNormalizado = nome == null || nome.isBlank() ? "Jogador" : nome.trim();
        String modoNormalizado = modo == null ? "ai" : modo;
        if ("ai".equals(modoNormalizado)) {
            if (nomeNormalizado.length() < 2 || nomeNormalizado.length() > 18) {
                throw new IllegalArgumentException("O nome deve ter entre 2 e 18 caracteres.");
            }
            novaPartida(List.of(nomeNormalizado), 1);
            return;
        }
        if ("human".equals(modoNormalizado)) {
            throw new IllegalArgumentException("Informe os nomes dos jogadores humanos.");
        }
        throw new IllegalArgumentException("Modo de partida inválido.");
    }

    /** Inicia uma partida local para 2 a 5 jogadores humanos. */
    public synchronized void novaPartida(List<String> nomes) {
        if (nomes == null || nomes.size() < 2 || nomes.size() > 5) {
            throw new IllegalArgumentException("A partida precisa de 2 a 5 jogadores.");
        }
        List<String> nomesNormalizados = nomes.stream()
                .map(nome -> nome == null ? "" : nome.trim())
                .toList();
        if (nomesNormalizados.stream().anyMatch(nome -> nome.length() < 2 || nome.length() > 18)) {
            throw new IllegalArgumentException("Cada nome deve ter entre 2 e 18 caracteres.");
        }
        iniciarPartida(nomesNormalizados, 0, "human");
    }

    /** Inicia uma partida com humanos nomeados e a quantidade escolhida de IAs. */
    public synchronized void novaPartida(List<String> nomes, int quantidadeIAs) {
        if (nomes == null || nomes.isEmpty() || nomes.size() > 4) {
            throw new IllegalArgumentException("A partida precisa de 1 a 4 jogadores humanos.");
        }
        if (quantidadeIAs < 1 || quantidadeIAs > 4) {
            throw new IllegalArgumentException("A partida precisa de 1 a 4 inteligências artificiais.");
        }
        if (nomes.size() + quantidadeIAs > 5) {
            throw new IllegalArgumentException("A partida pode ter no máximo 5 participantes.");
        }
        List<String> nomesNormalizados = nomes.stream()
                .map(nome -> nome == null ? "" : nome.trim())
                .toList();
        if (nomesNormalizados.stream().anyMatch(nome -> nome.length() < 2 || nome.length() > 18)) {
            throw new IllegalArgumentException("Cada nome deve ter entre 2 e 18 caracteres.");
        }
        iniciarPartida(nomesNormalizados, quantidadeIAs, "ai");
    }

    /** Inicia uma partida so de IAs (2 a 5); as jogadas saem por {@link #avancarTurnoIA()}. */
    public synchronized void novaPartidaEntreIAs(int quantidade) {
        if (quantidade < 2 || quantidade > 5) {
            throw new IllegalArgumentException("A partida precisa de 2 a 5 inteligências artificiais.");
        }
        iniciarPartida(List.of(), quantidade, MODO_ENTRE_IAS);
    }

    private void iniciarPartida(List<String> nomes, int quantidadeIAs, String modo) {
        this.modo = modo == null ? "ai" : modo;
        partida = new Partida(new Baralho());
        Cor[] cores = Cor.values();
        for (int i = 0; i < nomes.size(); i++) {
            partida.adicionarJogador(new Jogador(nomes.get(i), cores[i], new EstrategiaHumana()));
        }
        for (int i = 0; i < quantidadeIAs; i++) {
            partida.adicionarJogador(new Jogador("IA " + (i + 1), cores[nomes.size() + i],
                    new EstrategiaIA(2)));
        }
        partida.iniciar();
        jogadaPendente = null;
        mensagem = "";
        comprarProxima();
        executarTurnosIA();
    }

    public synchronized boolean temPartida() {
        return partida != null;
    }

    public synchronized Partida getPartida() {
        return partida;
    }

    public synchronized Jogador getJogador() {
        return partida == null ? null : partida.getJogadorAtual();
    }

    public synchronized Peca getPecaAtual() {
        return pecaAtual;
    }

    public synchronized String getMensagem() {
        return mensagem;
    }

    public synchronized String getModo() {
        return modo;
    }

    public synchronized boolean vezDaIA() {
        return partida != null && getJogador() != null
                && getJogador().getEstrategia() instanceof EstrategiaIA;
    }

    public synchronized Jogada getJogadaPendente() {
        return jogadaPendente;
    }

    /** RF02.4: gira a cartela na mao. As posicoes validas mudam junto. */
    public synchronized void rotacionar(boolean paraDireita) {
        if (pecaAtual == null || jogadaPendente != null) {
            return;
        }
        if (paraDireita) {
            pecaAtual.rotacionarDireita();
        } else {
            pecaAtual.rotacionarEsquerda();
        }
        mensagem = "";
    }

    /**
     * Tenta posicionar a cartela na coordenada indicada. Em caso de sucesso,
     * registra a jogada e aguarda a escolha opcional de meeple.
     * @return true se o encaixe era valido.
     */
    public synchronized boolean colocar(int x, int y) {
        if (pecaAtual == null || jogadaPendente != null) {
            return false;
        }
        Posicao posicao = new Posicao(x, y);
        Jogada jogada = new Jogada(getJogador(), pecaAtual, posicao, pecaAtual.getRotacao());
        if (!jogada.colocarCartela(partida.getTabuleiro())) {
            mensagem = "Essa posicao nao aceita a cartela nesta rotacao.";
            return false;
        }
        partida.rodadaAtual().registrar(jogada);
        jogadaPendente = jogada;
        pecaAtual = null;
        mensagem = "";
        if (!jogadaPendente.temOpcaoDeSeguidor()) {
            concluirJogada(-1);
        }
        return true;
    }

    /**
     * Conclui a jogada alocando opcionalmente um meeple na peça recém-colocada,
     * pontuando regiões fechadas e comprando a peça seguinte.
     *
     * @param indiceSegmento índice do segmento na peça, ou -1 para não alocar.
     * @return true se a escolha era válida e a jogada foi concluída.
     */
    public synchronized boolean concluirJogada(int indiceSegmento) {
        if (jogadaPendente == null) {
            mensagem = "Coloque uma peça antes de concluir a jogada.";
            return false;
        }
        if (indiceSegmento >= 0) {
            List<Segmento> segmentos = jogadaPendente.getPeca().getSegmentos();
            if (indiceSegmento >= segmentos.size()) {
                mensagem = "O segmento escolhido não existe nesta peça.";
                return false;
            }
            Segmento segmento = segmentos.get(indiceSegmento);
            Regiao regiao = segmento.getRegiao();
            if (regiao == null || regiao.estaOcupada()) {
                mensagem = "Esta região já possui um meeple e não pode receber outro.";
                return false;
            }
            jogadaPendente.setSegmentoEscolhido(segmento);
            if (!jogadaPendente.colocarSeguidor()) {
                mensagem = getJogador().temMeepleDisponivel()
                        ? "Não foi possível alocar o meeple neste segmento."
                        : "Você não tem meeples disponíveis.";
                return false;
            }
        } else if (indiceSegmento != -1) {
            mensagem = "Índice de segmento inválido.";
            return false;
        }

        jogadaPendente.pontuarRegioesConcluidas(partida.getTabuleiro());
        jogadaPendente = null;
        mensagem = "";
        partida.passarVez();
        comprarProxima();
        if (!executandoTurnosIA) {
            executarTurnosIA();
        }
        return true;
    }

    private void executarTurnosIA() {
        if (executandoTurnosIA || MODO_ENTRE_IAS.equals(modo)) {
            return;
        }
        executandoTurnosIA = true;
        try {
            while (vezDaIA() && pecaAtual != null && jogadaPendente == null) {
                jogarUmTurnoIA();
            }
        } finally {
            executandoTurnosIA = false;
        }
    }

    /**
     * Joga exatamente um turno na partida so de IAs, para a tela mostrar a
     * partida passo a passo.
     *
     * @return false se a partida ja tinha acabado.
     */
    public synchronized boolean avancarTurnoIA() {
        if (!MODO_ENTRE_IAS.equals(modo)) {
            throw new IllegalStateException("avancar turno so existe na partida entre IAs");
        }
        if (partida == null) {
            throw new IllegalStateException("nenhuma partida em andamento");
        }
        if (encerrada()) {
            return false;
        }
        executandoTurnosIA = true;
        try {
            jogarUmTurnoIA();
        } finally {
            executandoTurnosIA = false;
        }
        return true;
    }

    /** Executa a jogada completa (cartela e meeple) da IA da vez. */
    private void jogarUmTurnoIA() {
        EstrategiaIA estrategia = (EstrategiaIA) getJogador().getEstrategia();
        Jogada escolhida = estrategia.escolherJogada(partida, pecaAtual);
        if (escolhida == null) {
            throw new IllegalStateException("A IA não encontrou uma jogada para uma peça válida.");
        }
        if (!colocar(escolhida.getPosicao().getX(), escolhida.getPosicao().getY())) {
            throw new IllegalStateException("A jogada escolhida pela IA não é válida.");
        }
        if (jogadaPendente != null
                && !concluirJogada(estrategia.escolherSegmento(jogadaPendente))) {
            throw new IllegalStateException("A IA não conseguiu concluir a própria jogada.");
        }
    }

    /** @return segmentos distintos da peça recém-colocada para a escolha do meeple. */
    public synchronized List<Segmento> segmentosDaJogadaPendente() {
        if (jogadaPendente == null) {
            return new ArrayList<>();
        }
        List<Segmento> disponiveis = new ArrayList<>();
        Set<Regiao> visitadas = new HashSet<>();
        for (Segmento segmento : jogadaPendente.getPeca().getSegmentos()) {
            if (!segmento.permiteColocacaoDeMeeple()) {
                continue;
            }
            Regiao regiao = segmento.getRegiao();
            if (regiao != null && visitadas.add(regiao)) {
                disponiveis.add(segmento);
            }
        }
        return disponiveis;
    }

    /**
     * Compra do topo do baralho ate achar uma cartela que caiba em algum lugar
     * do tabuleiro em alguma rotacao. As que nao cabem sao descartadas (RF02.3).
     */
    public synchronized void comprarProxima() {
        int descartadas = 0;
        while (true) {
            Peca peca = partida.comprarPeca();
            if (peca == null) {
                pecaAtual = null;
                partida.contagemFinal();
                if (descartadas > 0) {
                    mensagem = textoDescarte(descartadas) + " O baralho acabou.";
                }
                return;
            }
            if (ajustarParaRotacaoJogavel(peca)) {
                pecaAtual = peca;
                if (descartadas > 0) {
                    mensagem = textoDescarte(descartadas);
                }
                return;
            }
            partida.getBaralho().descartar(peca);
            descartadas++;
        }
    }

    private String textoDescarte(int quantidade) {
        return quantidade == 1
                ? "Uma cartela sem encaixe foi descartada."
                : quantidade + " cartelas sem encaixe foram descartadas.";
    }

    /**
     * Procura uma rotacao jogavel para a cartela recem-comprada e deixa a peca
     * nela, para o jogador sempre receber a cartela numa orientacao que cabe em
     * algum lugar. Ele continua livre para gira-la.
     *
     * @return false se nenhuma das 4 rotacoes serve; nesse caso a cartela volta
     *         sozinha a rotacao original e deve ser descartada (RF02.3).
     */
    private boolean ajustarParaRotacaoJogavel(Peca peca) {
        Tabuleiro tabuleiro = partida.getTabuleiro();
        for (int giro = 0; giro < 4; giro++) {
            if (!tabuleiro.posicoesCandidatas(peca).isEmpty()) {
                return true;
            }
            peca.rotacionarDireita();
        }
        return false;
    }

    /** @return as posicoes onde a cartela da mao encaixa na rotacao atual (RNF04.1). */
    public synchronized List<Posicao> candidatas() {
        if (pecaAtual == null) {
            return new ArrayList<>();
        }
        return partida.getTabuleiro().posicoesCandidatas(pecaAtual);
    }

    /** RF01.5: a partida acaba quando nao ha mais cartela jogavel. */
    public synchronized boolean encerrada() {
        return partida == null || (pecaAtual == null && jogadaPendente == null);
    }
}
