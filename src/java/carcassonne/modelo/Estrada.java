package carcassonne.modelo;

/** Estrada: fecha quando as duas pontas esbarram em cruzamentos, cidades ou lacos. */
public class Estrada extends Regiao {

    public Estrada() {
        super(TipoTerreno.ESTRADA);
    }

    @Override
    public boolean estaCompleta() {
        return pontasAbertas == 0;
    }

    /** RF05.3 e RF05.4: 1 ponto por cartela, completa ou nao. */
    @Override
    public int calcularPontos(boolean fimDePartida) {
        return getQuantidadeDeCartelas();
    }
}
