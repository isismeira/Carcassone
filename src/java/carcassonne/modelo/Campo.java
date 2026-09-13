package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Campo: nunca fecha durante a partida, so pontua na contagem final (RF05.4). */
public class Campo extends Regiao {
    private final Set<Cidade> cidadesAdjacentes = new LinkedHashSet<>();

    public Campo() {
        super(TipoTerreno.CAMPO);
    }

    public List<Cidade> getCidadesAdjacentes() {
        return Collections.unmodifiableList(new ArrayList<>(cidadesAdjacentes));
    }

    public void adicionarCidadeAdjacente(Cidade cidade) {
        cidadesAdjacentes.add(cidade);
    }

    @Override
    protected void absorver(Regiao outra) {
        if (outra instanceof Campo campo) {
            cidadesAdjacentes.addAll(campo.cidadesAdjacentes);
        }
    }

    /** RF04.5: o fazendeiro fica no tabuleiro ate o fim, entao o campo nunca "completa". */
    @Override
    public boolean estaCompleta() {
        return false;
    }

    /** RF05.4: 3 pontos por cidade concluida que o campo alimenta. */
    @Override
    public int calcularPontos(boolean fimDePartida) {
        if (!fimDePartida) {
            return 0;
        }
        return (int) cidadesAdjacentes.stream().filter(Cidade::estaCompleta).count() * 3;
    }
}
