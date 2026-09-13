package carcassonne.modelo;

import java.util.ArrayDeque;
import java.util.Deque;

/** Participante da partida, com sua reserva de meeples e sua pontuacao. */
public final class Jogador {
    /** RF01.3: cada jogador comeca com 7 seguidores. */
    public static final int MEEPLES_INICIAIS = 7;

    private final String nome;
    private final Cor cor;
    private int pontos;
    private final Deque<Meeple> meeples = new ArrayDeque<>();
    private Estrategia estrategia;

    public Jogador(String nome, Cor cor, Estrategia estrategia) {
        this.nome = nome;
        this.cor = cor;
        this.estrategia = estrategia;
        for (int i = 0; i < MEEPLES_INICIAIS; i++) {
            meeples.add(new Meeple(this));
        }
    }

    public String getNome() {
        return nome;
    }

    public Cor getCor() {
        return cor;
    }

    public int getPontos() {
        return pontos;
    }

    public Estrategia getEstrategia() {
        return estrategia;
    }

    public void setEstrategia(Estrategia estrategia) {
        this.estrategia = estrategia;
    }

    public int getMeeplesDisponiveis() {
        return meeples.size();
    }

    public boolean temMeepleDisponivel() {
        return !meeples.isEmpty();
    }

    /** @return um meeple da reserva, ou null se ela estiver vazia. */
    public Meeple retirarMeeple() {
        return meeples.poll();
    }

    public void devolverMeeple(Meeple meeple) {
        if (meeple != null && !meeples.contains(meeple)) {
            meeples.add(meeple);
        }
    }

    public void marcarPontos(int quantidade) {
        pontos += quantidade;
    }

    @Override
    public String toString() {
        return nome + " (" + cor + ", " + pontos + " pts)";
    }
}
