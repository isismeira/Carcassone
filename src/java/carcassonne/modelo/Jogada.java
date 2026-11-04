package carcassonne.modelo;

import java.util.ArrayList;
import java.util.List;

/** Uma acao completa de um jogador: posicionar a cartela e, opcionalmente, um meeple. */
public class Jogada {
    private final Jogador jogador;
    private final Peca peca;
    private final Posicao posicao;
    private int rotacao;
    private Meeple meepleColocado;
    private Segmento segmentoEscolhido;

    public Jogada(Jogador jogador, Peca peca, Posicao posicao, int rotacao) {
        this.jogador = jogador;
        this.peca = peca;
        this.posicao = posicao;
        this.rotacao = rotacao;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public Peca getPeca() {
        return peca;
    }

    public Posicao getPosicao() {
        return posicao;
    }

    public int getRotacao() {
        return rotacao;
    }

    public void setRotacao(int rotacao) {
        this.rotacao = rotacao;
    }

    public Meeple getMeepleColocado() {
        return meepleColocado;
    }

    public Segmento getSegmentoEscolhido() {
        return segmentoEscolhido;
    }

    public void setSegmentoEscolhido(Segmento segmentoEscolhido) {
        this.segmentoEscolhido = segmentoEscolhido;
    }

    /** @return true se a jogada oferece ao jogador ao menos uma regiao livre. */
    public boolean temOpcaoDeSeguidor() {
        return jogador.temMeepleDisponivel() && peca.getSegmentos().stream()
                .filter(Segmento::permiteColocacaoDeMeeple)
                .map(Segmento::getRegiao)
                .filter(regiao -> regiao != null)
                .distinct()
                .anyMatch(regiao -> !regiao.estaOcupada());
    }

    /** @return true se a cartela entrou no tabuleiro. */
    public boolean colocarCartela(Tabuleiro tabuleiro) {
        return tabuleiro.colocar(peca, posicao);
    }

    /**
     * Aloca um meeple no segmento escolhido, respeitando reserva e RF04.3
     * (regiao ja ocupada nao aceita novo seguidor).
     * @return true se o meeple foi colocado.
     */
    public boolean colocarSeguidor() {
        if (segmentoEscolhido == null || !peca.getSegmentos().contains(segmentoEscolhido)
                || !segmentoEscolhido.permiteColocacaoDeMeeple()
                || segmentoEscolhido.getRegiao() == null
                || segmentoEscolhido.getRegiao().estaOcupada()
                || !jogador.temMeepleDisponivel()) {
            return false;
        }
        Regiao regiao = segmentoEscolhido.getRegiao();
        if (regiao != null && regiao.estaOcupada()) {
            return false;
        }
        Meeple meeple = jogador.retirarMeeple();
        if (meeple == null) {
            return false;
        }
        if (!meeple.ocupar(segmentoEscolhido)) {
            jogador.devolverMeeple(meeple);
            return false;
        }
        meepleColocado = meeple;
        return true;
    }

    /**
     * Pontua as regioes que fecharam com esta jogada e devolve os meeples
     * envolvidos (RF05.1, RF05.2 e RF04.4).
     * @return o total distribuido.
     */
    public int pontuarRegioesConcluidas(Tabuleiro tabuleiro) {
        int total = 0;
        for (Regiao regiao : tabuleiro.regioesConcluidas()) {
            total += distribuir(regiao, false);
        }
        return total;
    }

    /**
     * RF05.2: quem tiver mais meeples leva os pontos; em caso de empate, todos
     * os empatados recebem a pontuacao integral.
     */
    static int distribuir(Regiao regiao, boolean fimDePartida) {
        regiao.marcarComoPontuada();
        int pontos = regiao.calcularPontos(fimDePartida);
        List<Jogador> vencedores = donosComMaioria(regiao);
        for (Jogador jogador : vencedores) {
            jogador.marcarPontos(pontos);
        }
        if (regiao.getTipo() != TipoTerreno.CAMPO) {
            regiao.devolverMeeples();
        }
        return pontos * vencedores.size();
    }

    private static List<Jogador> donosComMaioria(Regiao regiao) {
        List<Jogador> vencedores = new ArrayList<>();
        int maior = 0;
        for (Meeple meeple : regiao.getMeeples()) {
            int quantidade = (int) regiao.getMeeples().stream()
                    .filter(outro -> outro.getDono() == meeple.getDono())
                    .count();
            if (quantidade > maior) {
                maior = quantidade;
                vencedores.clear();
                vencedores.add(meeple.getDono());
            } else if (quantidade == maior && !vencedores.contains(meeple.getDono())) {
                vencedores.add(meeple.getDono());
            }
        }
        return vencedores;
    }
}
