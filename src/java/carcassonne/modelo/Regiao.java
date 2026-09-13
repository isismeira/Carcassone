package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Conjunto conexo de segmentos do mesmo terreno espalhados por uma ou mais
 * cartelas. Abstrata de proposito: cada terreno fecha e pontua de um jeito
 * diferente (RNF01.3).
 */
public abstract class Regiao {
    protected final TipoTerreno tipo;
    protected int pontasAbertas;
    protected boolean pontuada;
    protected final List<Segmento> segmentos = new ArrayList<>();
    protected final List<Meeple> meeples = new ArrayList<>();

    protected Regiao(TipoTerreno tipo) {
        this.tipo = tipo;
    }

    public TipoTerreno getTipo() {
        return tipo;
    }

    public int getPontasAbertas() {
        return pontasAbertas;
    }

    public boolean foiPontuada() {
        return pontuada;
    }

    public void marcarComoPontuada() {
        this.pontuada = true;
    }

    public List<Segmento> getSegmentos() {
        return Collections.unmodifiableList(segmentos);
    }

    public List<Meeple> getMeeples() {
        return Collections.unmodifiableList(meeples);
    }

    /** @return true quando a regiao esta fechada e pode ser pontuada. */
    public abstract boolean estaCompleta();

    /**
     * @param fimDePartida true para a contagem final, que usa tabela reduzida (RF05.4).
     * @return os pontos que a regiao vale.
     */
    public abstract int calcularPontos(boolean fimDePartida);

    public void adicionarSegmento(Segmento segmento) {
        if (segmentos.contains(segmento)) {
            return;
        }
        segmentos.add(segmento);
        segmento.setRegiao(this);
        pontasAbertas += segmento.quantidadeDeLados();
    }

    /**
     * Absorve outra regiao do mesmo tipo, unificando areas antes isoladas (RF03.5).
     * A outra regiao fica vazia e deve ser descartada pelo chamador.
     */
    public void fundirCom(Regiao outra) {
        if (outra == this) {
            return;
        }
        for (Segmento segmento : new ArrayList<>(outra.segmentos)) {
            segmentos.add(segmento);
            segmento.setRegiao(this);
        }
        pontasAbertas += outra.pontasAbertas;
        meeples.addAll(outra.meeples);
        outra.segmentos.clear();
        outra.meeples.clear();
        outra.pontasAbertas = 0;
        absorver(outra);
    }

    /** Gancho para as subclasses somarem os proprios acumuladores durante a fusao. */
    protected void absorver(Regiao outra) {
        // por padrao nao ha estado extra a transferir
    }

    /** Registra que duas bordas desta regiao se encostaram, fechando duas pontas. */
    public void fecharPontas(int quantidade) {
        pontasAbertas = Math.max(0, pontasAbertas - quantidade);
    }

    /** RF04.3: uma regiao ja ocupada nao aceita novos meeples. */
    public boolean estaOcupada() {
        return !meeples.isEmpty();
    }

    public void adicionarMeeple(Meeple meeple) {
        meeples.add(meeple);
    }

    /** @return quantas cartelas distintas a regiao cobre. */
    public int getQuantidadeDeCartelas() {
        Set<Peca> pecas = new HashSet<>();
        for (Segmento segmento : segmentos) {
            if (segmento.getPeca() != null) {
                pecas.add(segmento.getPeca());
            }
        }
        return pecas.size();
    }

    /** RF04.4: devolve os meeples da regiao as reservas dos seus donos. */
    public void devolverMeeples() {
        for (Meeple meeple : new ArrayList<>(meeples)) {
            meeple.liberar();
        }
        meeples.clear();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[cartelas=" + getQuantidadeDeCartelas()
                + ", pontas=" + pontasAbertas + "]";
    }
}
