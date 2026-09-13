package carcassonne.modelo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Pilha de compra com as 72 cartelas do jogo base (RF02.1).
 *
 * <p>O catalogo das cartelas vive aqui, em tabela estatica, para nao acrescentar
 * classes fora do diagrama. Cada linha descreve os 9 setores da cartela na ordem
 * NORTE, LESTE, SUL, OESTE, NORDESTE, SUDESTE, SUDOESTE, NOROESTE, CENTRO,
 * usando os codigos C=cidade, E=estrada, A=campo, M=mosteiro e O=nenhum.
 * Os dados vem do projeto tsaglam/Carcassonne (EPL-2.0), preset
 * "Carcassonne Original", completado com as 2 cartelas de mosteiro com estrada
 * para fechar exatamente 72.</p>
 */
public class Baralho {
    public static final int TOTAL_DE_CARTELAS = 72;

    private static final String[][] CATALOGO = {
        // {nome, quantidade, setores}
        {"CastleCenter",         "1", "CCCCCCCCC"},
        {"CastleCenterEntry",    "3", "CCECCAACC"},
        {"CastleCenterSide",     "4", "CCACCAACC"},
        {"CastleEdge",           "5", "CCAACAAAA"},
        {"CastleEdgeRoad",       "4", "CCEECAAAE"},
        {"CastleSides",          "3", "CACAAAAAA"},
        {"CastleSidesEdge",      "2", "CAACAAAOA"},
        {"CastleTube",           "3", "ACACAAAAC"},
        {"CastleWall",           "5", "CAAAAAAAA"},
        {"CastleWallCurveLeft",  "3", "CAEEAAAAE"},
        {"CastleWallCurveRight", "3", "CEEAAAAAE"},
        {"CastleWallJunction",   "3", "CEEEAAAAO"},
        {"CastleWallRoad",       "4", "CEAEAAAAE"},
        {"Monastery",            "4", "AAAAAAAAM"},
        {"MonasteryCastle",      "1", "CCCCCCCCM"},
        {"MonasteryRoad",        "2", "AAEAAAAAM"},
        {"Road",                 "8", "EAEAAAAAE"},
        {"RoadCurve",            "9", "AAEEAAAAE"},
        {"RoadJunctionLarge",    "1", "EEEEAAAAO"},
        {"RoadJunctionSmall",    "4", "AEEEAAAAO"}
    };

    /** Cartela de inicio padrao do jogo base: muralha ao norte com estrada passando. */
    private static final String TIPO_INICIAL = "CastleWallRoad";

    /** Quantos dos 9 setores precisam ser cidade para a cartela ganhar brasao. */
    private static final int SETORES_PARA_BRASAO = 6;

    private final Deque<Peca> pecas = new ArrayDeque<>();
    private final List<Peca> descartadas = new ArrayList<>();
    private final Random sorteio;

    public Baralho() {
        this(new Random());
    }

    public Baralho(Random sorteio) {
        this.sorteio = sorteio;
        montar();
    }

    private void montar() {
        for (String[] linha : CATALOGO) {
            String nome = linha[0];
            int quantidade = Integer.parseInt(linha[1]);
            for (int i = 0; i < quantidade; i++) {
                pecas.add(criarPeca(nome, linha[2], nome + "#" + (i + 1), false));
            }
        }
    }

    /** Embaralha a pilha de compra (RF02.1). */
    public void embaralhar() {
        List<Peca> lista = new ArrayList<>(pecas);
        Collections.shuffle(lista, sorteio);
        pecas.clear();
        pecas.addAll(lista);
    }

    /** @return a cartela do topo, ou null se a pilha acabou (RF02.2). */
    public Peca comprar() {
        return pecas.poll();
    }

    /** RF02.3: cartela sem encaixe sai do jogo. */
    public void descartar(Peca peca) {
        descartadas.add(peca);
    }

    public boolean estaVazio() {
        return pecas.isEmpty();
    }

    public int quantidadeRestante() {
        return pecas.size();
    }

    public int quantidadeDescartada() {
        return descartadas.size();
    }

    /** @return a cartela que abre o tabuleiro na posicao (0,0), conforme o RF03.1. */
    public static Peca criarPecaInicial() {
        for (String[] linha : CATALOGO) {
            if (linha[0].equals(TIPO_INICIAL)) {
                return criarPeca(linha[0], linha[2], TIPO_INICIAL + "#inicial", true);
            }
        }
        throw new IllegalStateException("cartela inicial ausente do catalogo");
    }

    /** Monta uma cartela a partir da linha de catalogo, derivando os segmentos. */
    private static Peca criarPeca(String tipo, String setores, String id, boolean inicial) {
        boolean brasao = contarCidades(setores) >= SETORES_PARA_BRASAO;
        Peca peca = new Peca(id, tipo + "0.png", brasao, inicial);
        for (Segmento segmento : derivarSegmentos(setores)) {
            peca.adicionarSegmento(segmento);
        }
        return peca;
    }

    private static int contarCidades(String setores) {
        return (int) setores.chars().filter(c -> c == 'C').count();
    }

    /**
     * Descobre os segmentos da cartela a partir dos 9 setores.
     *
     * <p>Dois lados do mesmo terreno pertencem ao mesmo segmento quando o centro
     * da cartela e daquele terreno (o que liga todos os lados de uma vez) ou
     * quando sao lados vizinhos e o canto entre eles e do mesmo terreno. E por
     * isso que uma estrada reta forma um unico segmento e um cruzamento forma
     * varios, e que a cidade de canto liga os dois lados enquanto a cidade de
     * lados opostos permanece dividida.</p>
     */
    private static List<Segmento> derivarSegmentos(String setores) {
        List<Segmento> resultado = new ArrayList<>();
        char centro = setores.charAt(8);

        if (centro == 'M') {
            resultado.add(new Segmento(TipoTerreno.MOSTEIRO, List.of()));
        }

        for (char codigo : new char[] {'C', 'E', 'A'}) {
            int[] grupo = {0, 1, 2, 3};
            for (int lado = 0; lado < 4; lado++) {
                if (setores.charAt(lado) != codigo) {
                    grupo[lado] = -1;
                }
            }
            if (centro == codigo) {
                unirTodos(grupo);
            }
            // cantos: NE liga N-L, SE liga L-S, SW liga S-O, NW liga O-N
            for (int lado = 0; lado < 4; lado++) {
                int vizinho = (lado + 1) % 4;
                int canto = 4 + lado;
                if (grupo[lado] != -1 && grupo[vizinho] != -1 && setores.charAt(canto) == codigo) {
                    unir(grupo, lado, vizinho);
                }
            }
            adicionarGrupos(resultado, grupo, codigo);
        }
        return resultado;
    }

    private static void unirTodos(int[] grupo) {
        int primeiro = -1;
        for (int lado = 0; lado < 4; lado++) {
            if (grupo[lado] == -1) {
                continue;
            }
            if (primeiro == -1) {
                primeiro = lado;
            } else {
                unir(grupo, primeiro, lado);
            }
        }
    }

    private static void unir(int[] grupo, int a, int b) {
        int raizA = raiz(grupo, a);
        int raizB = raiz(grupo, b);
        if (raizA != raizB) {
            grupo[raizB] = raizA;
        }
    }

    private static int raiz(int[] grupo, int lado) {
        while (grupo[lado] != lado) {
            lado = grupo[lado];
        }
        return lado;
    }

    private static void adicionarGrupos(List<Segmento> resultado, int[] grupo, char codigo) {
        TipoTerreno tipo = switch (codigo) {
            case 'C' -> TipoTerreno.CIDADE;
            case 'E' -> TipoTerreno.ESTRADA;
            default -> TipoTerreno.CAMPO;
        };
        for (int raiz = 0; raiz < 4; raiz++) {
            if (grupo[raiz] == -1 || raiz(grupo, raiz) != raiz) {
                continue;
            }
            List<Lado> lados = new ArrayList<>();
            for (int lado = 0; lado < 4; lado++) {
                if (grupo[lado] != -1 && raiz(grupo, lado) == raiz) {
                    lados.add(Lado.values()[lado]);
                }
            }
            resultado.add(new Segmento(tipo, lados));
        }
    }

    /** @return o catalogo bruto, usado pelos testes para conferir o total. */
    static String[][] catalogo() {
        return CATALOGO;
    }
}
