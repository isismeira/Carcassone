package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cartela do jogo. Conhece apenas o nome do arquivo da sua arte (String), nunca
 * um objeto grafico, para nao acoplar o dominio a camada de Visao (RNF01.1).
 */
public class Peca {
    private final String id;
    private final boolean brasao;
    private final boolean inicial;
    private int rotacao;
    private final String imagem;
    private final List<Segmento> segmentos = new ArrayList<>();

    public Peca(String id, String imagem, boolean brasao, boolean inicial) {
        this.id = id;
        this.imagem = imagem;
        this.brasao = brasao;
        this.inicial = inicial;
        this.rotacao = 0;
    }

    public String getId() {
        return id;
    }

    public boolean temBrasao() {
        return brasao;
    }

    public boolean isInicial() {
        return inicial;
    }

    public int getRotacao() {
        return rotacao;
    }

    public String getImagem() {
        return imagem;
    }

    public List<Segmento> getSegmentos() {
        return Collections.unmodifiableList(segmentos);
    }

    public void adicionarSegmento(Segmento segmento) {
        segmento.setPeca(this);
        segmentos.add(segmento);
    }

    /** Gira a cartela 90 graus no sentido horario (RF02.4). */
    public void rotacionarDireita() {
        rotacao = (rotacao + 1) % 4;
        segmentos.forEach(segmento -> segmento.girar(true));
    }

    /** Gira a cartela 90 graus no sentido anti-horario (RF02.4). */
    public void rotacionarEsquerda() {
        rotacao = (rotacao + 3) % 4;
        segmentos.forEach(segmento -> segmento.girar(false));
    }

    /** @return o segmento que alcanca o lado indicado, ou null se nenhum alcanca. */
    public Segmento segmentoEm(Lado lado) {
        for (Segmento segmento : segmentos) {
            if (segmento.ocupa(lado)) {
                return segmento;
            }
        }
        return null;
    }

    /** @return o segmento que ocupa a quina indicada, ou null se nenhum ocupa. */
    public Segmento segmentoEm(Canto canto) {
        for (Segmento segmento : segmentos) {
            if (segmento.ocupa(canto)) {
                return segmento;
            }
        }
        return null;
    }

    /** @return o terreno exposto na borda indicada. */
    public TipoTerreno bordaEm(Lado lado) {
        Segmento segmento = segmentoEm(lado);
        return segmento == null ? null : segmento.getTipo();
    }

    /**
     * Valida a compatibilidade de bordas exigida pelo RF03.4.
     * @param vizinha cartela adjacente.
     * @param lado lado desta cartela que toca a vizinha.
     * @return true se os dois terrenos que se encostam sao iguais.
     */
    public boolean encaixaCom(Peca vizinha, Lado lado) {
        if (vizinha == null) {
            return true;
        }
        return bordaEm(lado) == vizinha.bordaEm(lado.oposto());
    }

    public boolean temMosteiro() {
        return segmentos.stream().anyMatch(s -> s.getTipo() == TipoTerreno.MOSTEIRO);
    }

    @Override
    public String toString() {
        return id + " (rot " + rotacao + ")";
    }
}
