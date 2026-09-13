package carcassonne.modelo;

/** Cidade: fecha quando toda a muralha esta conectada, sem lacunas. */
public class Cidade extends Regiao {
    private int brasoes;

    public Cidade() {
        super(TipoTerreno.CIDADE);
    }

    public int getBrasoes() {
        return brasoes;
    }

    @Override
    public void adicionarSegmento(Segmento segmento) {
        boolean novo = !segmentos.contains(segmento);
        super.adicionarSegmento(segmento);
        if (novo && segmento.getPeca() != null && segmento.getPeca().temBrasao()
                && ehPrimeiroSegmentoDaPeca(segmento)) {
            brasoes++;
        }
    }

    /** Uma cartela com brasao vale um unico brasao, mesmo tendo varios segmentos de cidade. */
    private boolean ehPrimeiroSegmentoDaPeca(Segmento segmento) {
        return segmentos.stream()
                .filter(outro -> outro.getPeca() == segmento.getPeca())
                .count() == 1;
    }

    @Override
    protected void absorver(Regiao outra) {
        if (outra instanceof Cidade cidade) {
            brasoes += cidade.brasoes;
        }
    }

    @Override
    public boolean estaCompleta() {
        return pontasAbertas == 0;
    }

    /** RF05.3: 2 pontos por cartela + 2 por brasao. RF05.4: metade disso na contagem final. */
    @Override
    public int calcularPontos(boolean fimDePartida) {
        int porCartela = fimDePartida ? 1 : 2;
        return getQuantidadeDeCartelas() * porCartela + brasoes * porCartela;
    }
}
