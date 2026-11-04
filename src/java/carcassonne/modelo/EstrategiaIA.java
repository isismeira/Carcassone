package carcassonne.modelo;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decisor autonomo guloso, isolado da Visao e baseado somente no estado do modelo.
 */
public class EstrategiaIA implements Estrategia {
    private final int dificuldade;

    public EstrategiaIA() {
        this(1);
    }

    public EstrategiaIA(int dificuldade) {
        this.dificuldade = dificuldade;
    }

    public int getDificuldade() {
        return dificuldade;
    }

    /** Testa as 4 rotacoes e mantem a orientacao correspondente a melhor jogada. */
    @Override
    public Jogada escolherJogada(Partida partida, Peca peca) {
        Tabuleiro tabuleiro = partida.getTabuleiro();
        Jogada melhor = null;
        int melhorNota = Integer.MIN_VALUE;
        int rotacaoOriginal = peca.getRotacao();
        for (int giro = 0; giro < 4; giro++) {
            for (Posicao posicao : tabuleiro.posicoesCandidatas(peca)) {
                Jogada candidata = new Jogada(partida.getJogadorAtual(), peca, posicao, peca.getRotacao());
                int nota = avaliarJogada(partida, candidata);
                if (nota > melhorNota) {
                    melhorNota = nota;
                    melhor = candidata;
                }
            }
            peca.rotacionarDireita();
        }
        while (peca.getRotacao() != rotacaoOriginal) {
            peca.rotacionarDireita();
        }
        if (melhor != null) {
            while (peca.getRotacao() != melhor.getRotacao()) {
                peca.rotacionarDireita();
            }
        }
        return melhor;
    }

    /**
     * Nota estrutural: recompensa fechamentos proprios e expansao de regioes
     * amigas, e evita entregar pontos imediatos a uma regiao dominada pelo rival.
     */
    public int avaliarJogada(Jogada jogada) {
        int nota = 0;
        for (Segmento segmento : jogada.getPeca().getSegmentos()) {
            Regiao regiao = segmento.getRegiao();
            if (regiao == null) {
                continue;
            }
            if (regiao.estaCompleta()) {
                nota += pontosParaJogador(regiao, jogada.getJogador()) * 2;
            }
            if (dificuldade > 1 && regiao.estaOcupada()) {
                nota += influenciaDaRegiao(regiao, jogada.getJogador());
            }
        }
        return nota;
    }

    private int avaliarJogada(Partida partida, Jogada jogada) {
        int nota = 0;
        Peca peca = jogada.getPeca();
        for (Segmento segmento : peca.getSegmentos()) {
            int conexoes = 0;
            Regiao vizinha = null;
            Set<Regiao> regioesLigadas = new HashSet<>();
            for (Lado lado : segmento.getLados()) {
                Peca pecaVizinha = partida.getTabuleiro().vizinha(jogada.getPosicao(), lado);
                if (pecaVizinha == null) {
                    continue;
                }
                Segmento segmentoVizinho = pecaVizinha.segmentoEm(lado.oposto());
                if (segmentoVizinho != null && segmentoVizinho.getTipo() == segmento.getTipo()) {
                    conexoes++;
                    vizinha = segmentoVizinho.getRegiao();
                    if (vizinha != null) {
                        regioesLigadas.add(vizinha);
                    }
                }
            }

            if (vizinha == null) {
                continue;
            }

            if (regioesLigadas.size() > 1) {
                nota += avaliarFusao(regioesLigadas, jogada.getJogador());
            }
            int pontasProjetadas = vizinha.getPontasAbertas()
                    + segmento.quantidadeDeLados() - 2 * conexoes;
            if (pontasProjetadas == 0 && !vizinha.foiPontuada()) {
                nota += pontosParaJogador(vizinha, jogada.getJogador()) * 3;
            } else if (dificuldade > 1) {
                nota += influenciaDaRegiao(vizinha, jogada.getJogador());
                if (vizinha.getTipo() == TipoTerreno.CIDADE && vizinha instanceof Cidade cidade) {
                    nota += cidade.getBrasoes() * 2;
                }
                if (vizinha.getTipo() == TipoTerreno.MOSTEIRO
                        && vizinha instanceof Mosteiro mosteiro) {
                    nota += mosteiro.getCartelasVizinhas();
                }
            }
        }
        return nota;
    }

    private int avaliarFusao(Set<Regiao> regioes, Jogador jogador) {
        int pontosEmDisputa = 0;
        Map<Jogador, Integer> meeplesPorJogador = new HashMap<>();
        boolean ocupada = false;
        for (Regiao regiao : regioes) {
            pontosEmDisputa += Math.max(1, regiao.calcularPontos(false));
            for (Meeple meeple : regiao.getMeeples()) {
                ocupada = true;
                meeplesPorJogador.merge(meeple.getDono(), 1, Integer::sum);
            }
        }
        if (!ocupada) {
            return jogador.temMeepleDisponivel() ? pontosEmDisputa : 0;
        }
        int meeplesDoJogador = meeplesPorJogador.getOrDefault(jogador, 0);
        int maiorOponente = meeplesPorJogador.entrySet().stream()
                .filter(entrada -> entrada.getKey() != jogador)
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);
        if (meeplesDoJogador == 0 || meeplesDoJogador < maiorOponente) {
            return -pontosEmDisputa;
        }
        return pontosEmDisputa * 2;
    }

    /**
     * Escolhe um segmento elegível da peça já colocada, aplicando uma postura
     * mais conservadora quando restam poucos meeples.
     *
     * @return índice do segmento, ou -1 para não alocar.
     */
    public int escolherSegmento(Jogada jogada) {
        Jogador jogador = jogada.getJogador();
        if (!jogador.temMeepleDisponivel()) {
            return -1;
        }

        int disponiveis = jogador.getMeeplesDisponiveis();
        int melhorIndice = -1;
        int melhorNota = 0;
        List<Segmento> segmentos = jogada.getPeca().getSegmentos();
        for (int indice = 0; indice < segmentos.size(); indice++) {
            Segmento segmento = segmentos.get(indice);
            Regiao regiao = segmento.getRegiao();
            if (!segmento.permiteColocacaoDeMeeple() || regiao == null || regiao.estaOcupada()) {
                continue;
            }
            int nota = avaliarAlocacao(regiao, disponiveis);
            if (nota > melhorNota) {
                melhorNota = nota;
                melhorIndice = indice;
            }
        }
        return melhorIndice;
    }

    private int avaliarAlocacao(Regiao regiao, int meeplesDisponiveis) {
        if (regiao.getTipo() == TipoTerreno.CAMPO) {
            if (meeplesDisponiveis < 5) {
                return 0;
            }
            return regiao.calcularPontos(true) + (regiao.getQuantidadeDeCartelas() > 1 ? 2 : 0);
        }

        if (meeplesDisponiveis <= 2 && !estaPertoDeFechar(regiao)) {
            return 0;
        }
        int nota = regiao.calcularPontos(false);
        if (regiao.estaCompleta()) {
            return nota + 100;
        }
        if (regiao.getTipo() == TipoTerreno.MOSTEIRO && regiao instanceof Mosteiro mosteiro) {
            nota += mosteiro.getCartelasVizinhas() * 2;
        } else if (regiao.getTipo() == TipoTerreno.CIDADE && regiao instanceof Cidade cidade) {
            nota += cidade.getBrasoes() * 2;
        }
        return nota;
    }

    private boolean estaPertoDeFechar(Regiao regiao) {
        return switch (regiao.getTipo()) {
            case ESTRADA -> regiao.getPontasAbertas() <= 1;
            case CIDADE -> regiao.getPontasAbertas() <= 2;
            case MOSTEIRO -> regiao instanceof Mosteiro mosteiro
                    && mosteiro.getCartelasVizinhas() >= 5;
            case CAMPO -> false;
        };
    }

    private int pontosParaJogador(Regiao regiao, Jogador jogador) {
        int pontos = regiao.calcularPontos(false);
        int maior = 0;
        int doJogador = 0;
        for (Meeple meeple : regiao.getMeeples()) {
            int quantidade = (int) regiao.getMeeples().stream()
                    .filter(outro -> outro.getDono() == meeple.getDono())
                    .count();
            maior = Math.max(maior, quantidade);
            if (meeple.getDono() == jogador) {
                doJogador = quantidade;
            }
        }
        if (doJogador == 0) {
            return 0;
        }
        return doJogador == maior ? pontos : -pontos;
    }

    private int influenciaDaRegiao(Regiao regiao, Jogador jogador) {
        if (!regiao.estaOcupada()) {
            return 0;
        }
        int pontos = Math.max(1, regiao.calcularPontos(false));
        return regiao.getMeeples().stream().anyMatch(meeple -> meeple.getDono() == jogador)
                ? regiao.getQuantidadeDeCartelas() + pontos
                : -regiao.getQuantidadeDeCartelas();
    }

    /** @return as jogadas possiveis, util para depurar a escolha da IA. */
    public List<Posicao> opcoes(Partida partida, Peca peca) {
        return partida.getTabuleiro().posicoesCandidatas(peca);
    }
}
