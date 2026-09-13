package carcassonne.modelo;

/**
 * Jogador de carne e osso: quem decide e a interface, entao esta estrategia nao
 * escolhe nada sozinha.
 */
public class EstrategiaHumana implements Estrategia {

    @Override
    public Jogada escolherJogada(Partida partida, Peca peca) {
        return null;
    }
}
