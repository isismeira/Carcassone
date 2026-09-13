package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Estado global do jogo: jogadores, baralho, tabuleiro e rodadas. */
public class Partida {
    private boolean encerrada;
    private final List<Jogador> jogadores = new ArrayList<>();
    private final List<Rodada> rodadas = new ArrayList<>();
    private final Baralho baralho;
    private final Tabuleiro tabuleiro = new Tabuleiro();
    private int indiceAtual;

    public Partida(Baralho baralho) {
        this.baralho = baralho;
    }

    public Partida() {
        this(new Baralho());
    }

    public void adicionarJogador(Jogador jogador) {
        jogadores.add(jogador);
    }

    public List<Jogador> getJogadores() {
        return Collections.unmodifiableList(jogadores);
    }

    public Jogador getJogadorAtual() {
        return jogadores.isEmpty() ? null : jogadores.get(indiceAtual);
    }

    public Baralho getBaralho() {
        return baralho;
    }

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    public List<Rodada> getRodadas() {
        return Collections.unmodifiableList(rodadas);
    }

    public boolean estaEncerrada() {
        return encerrada;
    }

    /** Embaralha e abre o tabuleiro com a cartela inicial em (0,0) (RF03.1). */
    public void iniciar() {
        baralho.embaralhar();
        tabuleiro.colocar(Baralho.criarPecaInicial(), new Posicao(0, 0));
        rodadas.add(new Rodada(1));
        encerrada = false;
    }

    /** @return a cartela do topo do baralho (RF02.2). */
    public Peca comprarPeca() {
        return baralho.comprar();
    }

    /** Abre uma nova rodada e a devolve. */
    public Rodada proximaRodada() {
        Rodada rodada = new Rodada(rodadas.size() + 1);
        rodadas.add(rodada);
        return rodada;
    }

    public Rodada rodadaAtual() {
        return rodadas.isEmpty() ? proximaRodada() : rodadas.get(rodadas.size() - 1);
    }

    /** Passa a vez ciclicamente e abre nova rodada quando a mesa completa a volta (RF01.2). */
    public void passarVez() {
        if (jogadores.isEmpty()) {
            return;
        }
        indiceAtual = (indiceAtual + 1) % jogadores.size();
        if (indiceAtual == 0 && rodadaAtual().estaEncerrada(jogadores.size())) {
            proximaRodada();
        }
    }

    /** RF01.5: acaba quando o baralho esvazia. */
    public boolean terminou() {
        return encerrada || baralho.estaVazio();
    }

    /** RF05.4: pontua o que sobrou no tabuleiro e encerra a partida. */
    public void contagemFinal() {
        for (Regiao regiao : tabuleiro.getRegioes()) {
            if (!regiao.foiPontuada()) {
                Jogada.distribuir(regiao, true);
            }
        }
        encerrada = true;
    }

    /** @return os jogadores com a maior pontuacao (pode haver empate). */
    public List<Jogador> vencedores() {
        List<Jogador> lideres = new ArrayList<>();
        int maior = Integer.MIN_VALUE;
        for (Jogador jogador : jogadores) {
            if (jogador.getPontos() > maior) {
                maior = jogador.getPontos();
                lideres.clear();
                lideres.add(jogador);
            } else if (jogador.getPontos() == maior) {
                lideres.add(jogador);
            }
        }
        return lideres;
    }
}
