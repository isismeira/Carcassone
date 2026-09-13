package carcassonne.modelo;

import java.util.List;

/**
 * Agente autonomo guloso (RF06.2). Vive no modelo, sem qualquer acesso a Visao
 * (RNF05.2). Nao e exercitado pela tela deste prototipo.
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

    /** Testa as 4 rotacoes em cada posicao livre e fica com a de maior nota. */
    @Override
    public Jogada escolherJogada(Partida partida, Peca peca) {
        Tabuleiro tabuleiro = partida.getTabuleiro();
        Jogada melhor = null;
        int melhorNota = Integer.MIN_VALUE;
        for (int giro = 0; giro < 4; giro++) {
            for (Posicao posicao : tabuleiro.posicoesCandidatas(peca)) {
                Jogada candidata = new Jogada(partida.getJogadorAtual(), peca, posicao, peca.getRotacao());
                int nota = avaliarJogada(candidata);
                if (nota > melhorNota) {
                    melhorNota = nota;
                    melhor = candidata;
                }
            }
            peca.rotacionarDireita();
        }
        return melhor;
    }

    /**
     * Nota da jogada: prioriza fechar regioes proprias e, com dificuldade maior,
     * tambem valoriza vizinhanca de regioes disputadas.
     */
    public int avaliarJogada(Jogada jogada) {
        int nota = 0;
        for (Segmento segmento : jogada.getPeca().getSegmentos()) {
            Regiao regiao = segmento.getRegiao();
            if (regiao == null) {
                continue;
            }
            if (regiao.estaCompleta()) {
                nota += regiao.calcularPontos(false) * 2;
            }
            if (dificuldade > 1 && regiao.estaOcupada()) {
                nota += regiao.getQuantidadeDeCartelas();
            }
        }
        return nota;
    }

    /** @return as jogadas possiveis, util para depurar a escolha da IA. */
    public List<Posicao> opcoes(Partida partida, Peca peca) {
        return partida.getTabuleiro().posicoesCandidatas(peca);
    }
}
