package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parte homogenea de uma cartela: um trecho de estrada, uma porcao de muralha,
 * um campo ou o mosteiro. E o elo intermediario da hierarquia
 * Peca -> Segmento -> Regiao exigida pelo RNF01.2.
 */
public class Segmento {
    private final TipoTerreno tipo;
    private final List<Lado> lados;
    private Peca peca;
    private Regiao regiao;

    public Segmento(TipoTerreno tipo, List<Lado> lados) {
        this.tipo = tipo;
        this.lados = new ArrayList<>(lados);
    }

    public TipoTerreno getTipo() {
        return tipo;
    }

    /** @return os lados ocupados, ja considerando a rotacao atual da peca. */
    public List<Lado> getLados() {
        return Collections.unmodifiableList(lados);
    }

    /** @return true se este segmento alcanca a borda indicada. */
    public boolean ocupa(Lado lado) {
        return lados.contains(lado);
    }

    public int quantidadeDeLados() {
        return lados.size();
    }

    public Peca getPeca() {
        return peca;
    }

    void setPeca(Peca peca) {
        this.peca = peca;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }

    /** Gira os lados ocupados em 90 graus. Chamado pela peca que contem o segmento. */
    void girar(boolean horario) {
        lados.replaceAll(lado -> horario ? lado.proximoHorario() : lado.proximoAntiHorario());
    }

    /** @return true se ha um meeple sobre este segmento. */
    public boolean estaOcupado() {
        return regiao != null && regiao.estaOcupada();
    }
}
