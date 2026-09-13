package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Uma volta completa da mesa: uma jogada por jogador (RF01.2). */
public class Rodada {
    private final int numero;
    private final List<Jogada> jogadas = new ArrayList<>();

    public Rodada(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    public List<Jogada> getJogadas() {
        return Collections.unmodifiableList(jogadas);
    }

    public void registrar(Jogada jogada) {
        jogadas.add(jogada);
    }

    /** @return true quando todos os jogadores ja jogaram nesta rodada. */
    public boolean estaEncerrada(int totalDeJogadores) {
        return jogadas.size() >= totalDeJogadores;
    }
}
