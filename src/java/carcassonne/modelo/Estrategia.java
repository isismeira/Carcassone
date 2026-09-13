package carcassonne.modelo;

/**
 * Como um jogador decide a proxima jogada. Permite trocar humano por IA sem
 * mexer no resto do modelo.
 */
public interface Estrategia {
    /**
     * @param partida estado atual do jogo.
     * @param peca cartela comprada neste turno.
     * @return a jogada escolhida, ou null quando a decisao vem da interface.
     */
    Jogada escolherJogada(Partida partida, Peca peca);
}
