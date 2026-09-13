package carcassonne.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grade de cartelas indexada por coordenada. Usa HashMap para consulta em O(1)
 * e crescimento ilimitado em qualquer direcao (RF03.2 e RNF02.1).
 */
public class Tabuleiro {
    private final Map<Posicao, Peca> pecas = new HashMap<>();
    private final List<Regiao> regioes = new ArrayList<>();

    /** @return a cartela naquela coordenada, ou null se estiver vazia. */
    public Peca pecaEm(Posicao posicao) {
        return pecas.get(posicao);
    }

    /** @return a cartela adjacente naquele lado, ou null. */
    public Peca vizinha(Posicao posicao, Lado lado) {
        return pecas.get(posicao.vizinha(lado));
    }

    public boolean espacoLivre(Posicao posicao) {
        return !pecas.containsKey(posicao);
    }

    public boolean estaVazio() {
        return pecas.isEmpty();
    }

    public Map<Posicao, Peca> getPecas() {
        return Collections.unmodifiableMap(pecas);
    }

    public List<Regiao> getRegioes() {
        return Collections.unmodifiableList(regioes);
    }

    /**
     * Validacao completa de encaixe: espaco livre (RF03.3), pelo menos uma
     * vizinha (RF03.3) e todas as bordas compartilhadas compativeis (RF03.4).
     */
    public boolean podeColocar(Peca peca, Posicao posicao) {
        if (peca == null || !espacoLivre(posicao)) {
            return false;
        }
        boolean temVizinha = false;
        for (Lado lado : Lado.values()) {
            Peca vizinha = vizinha(posicao, lado);
            if (vizinha == null) {
                continue;
            }
            temVizinha = true;
            if (!peca.encaixaCom(vizinha, lado)) {
                return false;
            }
        }
        return temVizinha || pecas.isEmpty();
    }

    /**
     * Posiciona a cartela e reorganiza as regioes afetadas.
     * @return true se o encaixe era valido.
     */
    public boolean colocar(Peca peca, Posicao posicao) {
        if (!podeColocar(peca, posicao)) {
            return false;
        }
        pecas.put(posicao, peca);
        integrarRegioes(peca, posicao);
        atualizarMosteiros(posicao);
        return true;
    }

    /**
     * @param peca cartela na rotacao que se quer testar.
     * @return todas as coordenadas livres onde ela encaixa agora. Alimenta o
     *         destaque visual exigido pelo RNF04.1.
     */
    public List<Posicao> posicoesCandidatas(Peca peca) {
        List<Posicao> candidatas = new ArrayList<>();
        for (Posicao vaga : posicoesVagas()) {
            if (podeColocar(peca, vaga)) {
                candidatas.add(vaga);
            }
        }
        return candidatas;
    }

    /** @return as coordenadas livres encostadas em alguma cartela ja posicionada. */
    public Set<Posicao> posicoesVagas() {
        Set<Posicao> vagas = new LinkedHashSet<>();
        for (Posicao ocupada : pecas.keySet()) {
            for (Lado lado : Lado.values()) {
                Posicao candidata = ocupada.vizinha(lado);
                if (espacoLivre(candidata)) {
                    vagas.add(candidata);
                }
            }
        }
        return vagas;
    }

    /** @return as regioes que fecharam e ainda nao foram pontuadas (RF05.1). */
    public List<Regiao> regioesConcluidas() {
        List<Regiao> concluidas = new ArrayList<>();
        for (Regiao regiao : regioes) {
            if (!regiao.foiPontuada() && regiao.estaCompleta()) {
                concluidas.add(regiao);
            }
        }
        return concluidas;
    }

    /**
     * Cria as regioes dos segmentos novos e funde com as vizinhas do mesmo
     * terreno, unificando areas antes isoladas (RF03.5).
     */
    private void integrarRegioes(Peca peca, Posicao posicao) {
        for (Segmento segmento : peca.getSegmentos()) {
            if (segmento.getRegiao() == null) {
                Regiao nova = criarRegiaoPara(segmento.getTipo());
                nova.adicionarSegmento(segmento);
                regioes.add(nova);
            }
        }
        for (Segmento segmento : peca.getSegmentos()) {
            for (Lado lado : segmento.getLados()) {
                Peca vizinha = vizinha(posicao, lado);
                if (vizinha == null) {
                    continue;
                }
                Segmento oposto = vizinha.segmentoEm(lado.oposto());
                if (oposto == null || oposto.getTipo() != segmento.getTipo()) {
                    continue;
                }
                // quem sobrevive a fusao e a regiao que ja estava no tabuleiro:
                // assim referencias antigas a ela continuam validas
                Regiao estabelecida = oposto.getRegiao();
                Regiao recemCriada = segmento.getRegiao();
                if (estabelecida != recemCriada) {
                    estabelecida.fundirCom(recemCriada);
                    regioes.remove(recemCriada);
                }
                // as duas bordas que se encostaram deixam de ser pontas abertas
                estabelecida.fecharPontas(2);
            }
        }
        ligarCamposACidades(peca);
    }

    /** Registra quais cidades cada campo da cartela alimenta, para o RF05.4. */
    private void ligarCamposACidades(Peca peca) {
        for (Segmento campo : peca.getSegmentos()) {
            if (campo.getTipo() != TipoTerreno.CAMPO || !(campo.getRegiao() instanceof Campo regiaoCampo)) {
                continue;
            }
            for (Segmento cidade : peca.getSegmentos()) {
                if (cidade.getTipo() == TipoTerreno.CIDADE && cidade.getRegiao() instanceof Cidade regiaoCidade) {
                    regiaoCampo.adicionarCidadeAdjacente(regiaoCidade);
                }
            }
        }
    }

    /** Recalcula quantas vizinhas cada mosteiro proximo ja tem (RF05.1). */
    private void atualizarMosteiros(Posicao posicao) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Posicao alvo = new Posicao(posicao.getX() + dx, posicao.getY() + dy);
                Peca peca = pecas.get(alvo);
                if (peca == null || !peca.temMosteiro()) {
                    continue;
                }
                for (Segmento segmento : peca.getSegmentos()) {
                    if (segmento.getTipo() == TipoTerreno.MOSTEIRO
                            && segmento.getRegiao() instanceof Mosteiro mosteiro) {
                        mosteiro.setCartelasVizinhas(contarVizinhas(alvo));
                    }
                }
            }
        }
    }

    private int contarVizinhas(Posicao centro) {
        int total = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (pecas.containsKey(new Posicao(centro.getX() + dx, centro.getY() + dy))) {
                    total++;
                }
            }
        }
        return total;
    }

    private Regiao criarRegiaoPara(TipoTerreno tipo) {
        return switch (tipo) {
            case CIDADE -> new Cidade();
            case ESTRADA -> new Estrada();
            case MOSTEIRO -> new Mosteiro();
            case CAMPO -> new Campo();
        };
    }

    /** @return quantas cartelas ja foram posicionadas. */
    public int quantidadeDePecas() {
        return pecas.size();
    }

    /** @return as regioes distintas presentes no tabuleiro. */
    public Set<Regiao> regioesDistintas() {
        return new HashSet<>(regioes);
    }
}
