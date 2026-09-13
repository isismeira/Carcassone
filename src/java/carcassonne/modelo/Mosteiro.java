package carcassonne.modelo;

/** Mosteiro: fecha quando as 8 cartelas ao redor estao no tabuleiro. */
public class Mosteiro extends Regiao {
    private static final int VIZINHAS_PARA_FECHAR = 8;
    private int cartelasVizinhas;

    public Mosteiro() {
        super(TipoTerreno.MOSTEIRO);
    }

    public int getCartelasVizinhas() {
        return cartelasVizinhas;
    }

    /** Atualizado pelo Tabuleiro sempre que uma cartela cai na vizinhanca 3x3. */
    public void setCartelasVizinhas(int cartelasVizinhas) {
        this.cartelasVizinhas = cartelasVizinhas;
    }

    @Override
    public boolean estaCompleta() {
        return cartelasVizinhas >= VIZINHAS_PARA_FECHAR;
    }

    /** RF05.3: 9 pontos fechado. RF05.4: 1 pela propria cartela + 1 por vizinha. */
    @Override
    public int calcularPontos(boolean fimDePartida) {
        return 1 + cartelasVizinhas;
    }
}
