package carcassonne.controle;

import carcassonne.modelo.Baralho;
import carcassonne.modelo.Cor;
import carcassonne.modelo.EstrategiaHumana;
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
    private Partida partida;
    private Jogador jogador;
    private Peca pecaAtual;
    private Jogada jogadaPendente;
    private String mensagem = "";
    private String modo = "ai";

    /**
     * Comeca uma partida nova: cria o jogador, embaralha, abre o tabuleiro com a
     * cartela inicial e ja compra a primeira cartela jogavel.
     */
    public synchronized void novaPartida(String nome, String modo) {
        this.modo = modo == null ? "ai" : modo;
        partida = new Partida(new Baralho());
        jogador = new Jogador(nome == null || nome.isBlank() ? "Jogador" : nome.trim(),
                Cor.VERMELHO, new EstrategiaHumana());
        partida.adicionarJogador(jogador);
        partida.iniciar();
        jogadaPendente = null;
        mensagem = "";
        comprarProxima();
    }

    public synchronized boolean temPartida() {
        return partida != null;
    }

    public synchronized Partida getPartida() {
        return partida;
    }

    public synchronized Jogador getJogador() {
        return jogador;
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
        Jogada jogada = new Jogada(jogador, pecaAtual, posicao, pecaAtual.getRotacao());
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
                mensagem = jogador.temMeepleDisponivel()
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
        comprarProxima();
        return true;
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
