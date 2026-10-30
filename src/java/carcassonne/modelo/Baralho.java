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
 * A quarta coluna declara os grupos de campo no formato lados:quinas, separando
 * campos por ponto e virgula; N/E/S/W representam lados e NE/SE/SW/NW, quinas.
 * Os dados vem do projeto tsaglam/Carcassonne (EPL-2.0), preset
 * "Carcassonne Original", completado com as 2 cartelas de mosteiro com estrada
 * para fechar exatamente 72.</p>
 */
public class Baralho {
    public static final int TOTAL_DE_CARTELAS = 72;

    private static final String[][] CATALOGO = {
        // {nome, quantidade, setores, grupos de campo (lados:quinas;...)}
        {"CastleCenter",         "1", "CCCCCCCCC", ""},
        {"CastleCenterEntry",    "3", "CCECCAACC", ":SE;:SW"},
        {"CastleCenterSide",     "4", "CCACCAACC", "S:SE,SW"},
        {"CastleEdge",           "5", "CCAACAAAA", "S,W:SE,SW,NW"},
        {"CastleEdgeRoad",       "4", "CCEECAAAE", ":SE;:SW;:NW"},
        {"CastleSides",          "3", "CACAAAAAA", "E,W:NE,SE,SW,NW"},
        {"CastleSidesEdge",      "2", "CAACAAAOA", "E,S:NE,SE,SW"},
        {"CastleTube",           "3", "ACACAAAAC", "N:NE,NW;S:SE,SW"},
        {"CastleWall",           "5", "CAAAAAAAA", "E,S,W:NE,SE,SW,NW"},
        {"CastleWallCurveLeft",  "3", "CAEEAAAAE", "E:NE,SE;:SW;:NW"},
        {"CastleWallCurveRight", "3", "CEEAAAAAE", "W:SW,NW;:NE;:SE"},
        {"CastleWallJunction",   "3", "CEEEAAAAO", ":NE;:SE;:SW;:NW"},
        {"CastleWallRoad",       "4", "CEAEAAAAE", "S:SE,SW;:NE;:NW"},
        {"Monastery",            "4", "AAAAAAAAM", "N,E,S,W:NE,SE,SW,NW"},
        {"MonasteryCastle",      "1", "CCCCCCCCM", ""},
        {"MonasteryRoad",        "2", "AAEAAAAAM", "N,E,W:NE,SE,SW,NW"},
        {"Road",                 "8", "EAEAAAAAE", "E:NE,SE;W:SW,NW"},
        {"RoadCurve",            "9", "AAEEAAAAE", "N,E:NE,SE,NW;:SW"},
        {"RoadJunctionLarge",    "1", "EEEEAAAAO", ":NE;:SE;:SW;:NW"},
        {"RoadJunctionSmall",    "4", "AEEEAAAAO", "N:NE,NW;:SE;:SW"}
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
                pecas.add(criarPeca(nome, linha[2], linha[3], nome + "#" + (i + 1), false));
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
                return criarPeca(linha[0], linha[2], linha[3], TIPO_INICIAL + "#inicial", true);
            }
        }
        throw new IllegalStateException("cartela inicial ausente do catalogo");
    }

    /** Monta uma cartela a partir da linha de catalogo, derivando os segmentos. */
    private static Peca criarPeca(
            String tipo, String setores, String topologiaCampos, String id, boolean inicial) {
        boolean brasao = contarCidades(setores) >= SETORES_PARA_BRASAO;
        Peca peca = new Peca(id, tipo + "0.png", brasao, inicial);
        for (Segmento segmento : derivarSegmentos(setores, topologiaCampos)) {
            peca.adicionarSegmento(segmento);
        }
        return peca;
    }

    private static int contarCidades(String setores) {
        return (int) setores.chars().filter(c -> c == 'C').count();
    }

    /** Deriva cidades e estradas dos setores e cria campos pela topologia declarada. */
    private static List<Segmento> derivarSegmentos(String setores, String topologiaCampos) {
        List<Segmento> resultado = new ArrayList<>();
        char centro = setores.charAt(8);

        if (centro == 'M') {
            resultado.add(new Segmento(TipoTerreno.MOSTEIRO, List.of()));
        }

        for (char codigo : new char[] {'C', 'E'}) {
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
        adicionarCampos(resultado, setores, topologiaCampos);
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
            default -> TipoTerreno.ESTRADA;
        };
        for (int raiz = 0; raiz < 4; raiz++) {
            if (grupo[raiz] == -1 || raiz(grupo, raiz) != raiz) {
                continue;
            }
            List<Lado> lados = new ArrayList<>();
            double somaX = 0;
            double somaY = 0;
            for (int lado = 0; lado < 4; lado++) {
                if (grupo[lado] != -1 && raiz(grupo, lado) == raiz) {
                    lados.add(Lado.values()[lado]);
                    double[] posicaoLado = coordenadaMeepleNoLado(Lado.values()[lado]);
                    somaX += posicaoLado[0];
                    somaY += posicaoLado[1];
                }
            }
            double posicaoX = somaX / lados.size();
            double posicaoY = somaY / lados.size();
            if (posicaoX == 0.5 && posicaoY == 0.5) {
                double[] posicaoLado = coordenadaMeepleNoLado(lados.get(0));
                posicaoX = posicaoLado[0];
                posicaoY = posicaoLado[1];
            }
            resultado.add(new Segmento(tipo, lados, List.of(), posicaoX, posicaoY));
        }
    }

    private static void adicionarCampos(
            List<Segmento> resultado, String setores, String topologiaCampos) {
        boolean[] setoresDeclarados = new boolean[8];
        if (!topologiaCampos.isEmpty()) {
            for (String definicao : topologiaCampos.split(";", -1)) {
                String[] partes = definicao.split(":", -1);
                if (partes.length != 2) {
                    throw new IllegalArgumentException("topologia de campo invalida: " + definicao);
                }
                List<Lado> lados = new ArrayList<>();
                List<Canto> cantos = new ArrayList<>();
                if (!partes[0].isEmpty()) {
                    for (String codigo : partes[0].split(",")) {
                        Lado lado = ladoDeCodigo(codigo);
                        validarSetorCampo(setores, lado.ordinal(), setoresDeclarados, codigo);
                        lados.add(lado);
                    }
                }
                if (!partes[1].isEmpty()) {
                    for (String codigo : partes[1].split(",")) {
                        Canto canto = cantoDeCodigo(codigo);
                        int indice = indiceSetorDoCanto(canto);
                        validarSetorCampo(setores, indice, setoresDeclarados, codigo);
                        cantos.add(canto);
                    }
                }
                double posicaoX = 0.5;
                double posicaoY = 0.5;
                if (!lados.isEmpty()) {
                    double somaX = 0;
                    double somaY = 0;
                    for (Lado lado : lados) {
                        double[] posicao = coordenadaMeepleNoLado(lado);
                        somaX += posicao[0];
                        somaY += posicao[1];
                    }
                    posicaoX = somaX / lados.size();
                    posicaoY = somaY / lados.size();
                    if (posicaoX == 0.5 && posicaoY == 0.5) {
                        double[] posicao = coordenadaMeepleNoLado(lados.get(0));
                        posicaoX = posicao[0];
                        posicaoY = posicao[1];
                    }
                }
                resultado.add(new Segmento(TipoTerreno.CAMPO, lados, cantos, posicaoX, posicaoY));
            }
        }
        for (int setor = 0; setor < 8; setor++) {
            if ((setores.charAt(setor) == 'A') != setoresDeclarados[setor]) {
                throw new IllegalArgumentException(
                        "topologia de campo incompleta ou invalida no setor " + setor);
            }
        }
    }

    private static void validarSetorCampo(
            String setores, int indice, boolean[] declarados, String codigo) {
        if (setores.charAt(indice) != 'A' || declarados[indice]) {
            throw new IllegalArgumentException("setor de campo invalido ou repetido: " + codigo);
        }
        declarados[indice] = true;
    }

    private static Lado ladoDeCodigo(String codigo) {
        return switch (codigo) {
            case "N" -> Lado.NORTE;
            case "E" -> Lado.LESTE;
            case "S" -> Lado.SUL;
            case "W" -> Lado.OESTE;
            default -> throw new IllegalArgumentException("lado desconhecido: " + codigo);
        };
    }

    private static Canto cantoDeCodigo(String codigo) {
        return switch (codigo) {
            case "NE" -> Canto.NORDESTE;
            case "SE" -> Canto.SUDESTE;
            case "SW" -> Canto.SUDOESTE;
            case "NW" -> Canto.NOROESTE;
            default -> throw new IllegalArgumentException("quina desconhecida: " + codigo);
        };
    }

    private static int indiceSetorDoCanto(Canto canto) {
        return switch (canto) {
            case NORDESTE -> 4;
            case SUDESTE -> 5;
            case SUDOESTE -> 6;
            case NOROESTE -> 7;
        };
    }

    private static double[] coordenadaMeepleNoLado(Lado lado) {
        return switch (lado) {
            case NORTE -> new double[] {0.5, 0.2};
            case LESTE -> new double[] {0.8, 0.5};
            case SUL -> new double[] {0.5, 0.8};
            case OESTE -> new double[] {0.2, 0.5};
        };
    }

    /** @return o catalogo bruto, usado pelos testes para conferir o total. */
    static String[][] catalogo() {
        return CATALOGO;
    }
}
