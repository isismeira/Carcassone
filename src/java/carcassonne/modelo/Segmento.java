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
    private final List<Canto> cantos;
    private final double posicaoX;
    private final double posicaoY;
    private Peca peca;
    private Regiao regiao;

    public Segmento(TipoTerreno tipo, List<Lado> lados) {
        this(tipo, lados, List.of(), 0.5, 0.5);
    }

    public Segmento(TipoTerreno tipo, List<Lado> lados, double posicaoX, double posicaoY) {
        this(tipo, lados, List.of(), posicaoX, posicaoY);
    }

    public Segmento(TipoTerreno tipo, List<Lado> lados, List<Canto> cantos) {
        this(tipo, lados, cantos, 0.5, 0.5);
    }

    public Segmento(TipoTerreno tipo, List<Lado> lados, List<Canto> cantos,
            double posicaoX, double posicaoY) {
        this.tipo = tipo;
        this.lados = new ArrayList<>(lados);
        this.cantos = new ArrayList<>(cantos);
        this.posicaoX = posicaoX;
        this.posicaoY = posicaoY;
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

    /** @return as quinas ocupadas por este segmento. */
    public List<Canto> getCantos() {
        return Collections.unmodifiableList(cantos);
    }

    public boolean ocupa(Canto canto) {
        return cantos.contains(canto);
    }

    public int quantidadeDeLados() {
        return lados.size();
    }

    /** @return true se o meeple pode ser colocado na borda ou no centro de mosteiro. */
    public boolean permiteColocacaoDeMeeple() {
        return tipo == TipoTerreno.MOSTEIRO ? lados.isEmpty() : !lados.isEmpty();
    }

    /** Coordenada horizontal normalizada no desenho original da cartela. */
    public double getPosicaoX() {
        return posicaoX;
    }

    /** Coordenada vertical normalizada no desenho original da cartela. */
    public double getPosicaoY() {
        return posicaoY;
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
        cantos.replaceAll(canto -> horario ? canto.proximoHorario() : canto.proximoAntiHorario());
    }

    /** @return true se ha um meeple sobre este segmento. */
    public boolean estaOcupado() {
        return regiao != null && regiao.estaOcupada();
    }
}
