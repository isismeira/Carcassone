package carcassonne;

import carcassonne.controle.ControladorDePartida;
import carcassonne.modelo.EstrategiaIA;
import carcassonne.modelo.Baralho;
import carcassonne.modelo.Campo;
import carcassonne.modelo.Canto;
import carcassonne.modelo.Cidade;
import carcassonne.modelo.Cor;
import carcassonne.modelo.EstrategiaHumana;
import carcassonne.modelo.Jogada;
import carcassonne.modelo.Jogador;
import carcassonne.modelo.Lado;
import carcassonne.modelo.Meeple;
import carcassonne.modelo.Mosteiro;
import carcassonne.modelo.Peca;
import carcassonne.modelo.Posicao;
import carcassonne.modelo.Regiao;
import carcassonne.modelo.Segmento;
import carcassonne.modelo.Tabuleiro;
import carcassonne.modelo.TipoMeeple;
import carcassonne.modelo.TipoTerreno;
import java.util.List;

/** Conferencia das regras do modelo, executada com {@code java carcassonne.Principal --testes}. */
public final class Testes {
    private static int passou;
    private static int falhou;

    private Testes() {
    }

    public static int executar() {
        baralhoTem72Cartelas();
        rotacaoCompletaVoltaAoOriginal();
        rotacaoGiraAsBordas();
        encaixeComparaTerrenos();
        segmentosDerivadosCorretamente();
        segmentosMantemPosicaoLocalNaPeca();
        meepleSoPodeFicarNasBordasOuNoMosteiro();
        camposEmCurvasFicamSeparadosPelasEstradas();
        camposQueSeEncontramEmQuinasSaoUnificados();
        camposDosDoisLadosDaEstradaAtravessamABorda();
        camposSoNaDiagonalNaoSeUnem();
        faixaDeCampoEntreCidadeEEstradaEUmCampoSo();
        naoColocaIsoladaNemSobrepoe();
        candidatasDependemDaRotacao();
        estradaFechaNasDuasPontas();
        meeplePontuaEVoltaAReserva();
        fazendeiroNaoPodeDuplicarNaRegiaoConectada();
        jogadaSemRegiaoLivreNaoOfereceMeeple();
        mosteiroFechaComOitoVizinhas();
        cidadeContaBrasao();
        descarteQuandoNaoCabe();
        semMeeplesContinuaAutomaticamente();
        turnosPassamPelosTresJogadores();
        iaExecutaOTurnoSemInteracao();
        partidaConfiguraMultiplosHumanosEIAs();
        cartelaSemEncaixeEmLugarNenhum();
        nenhumaCartelaSeperde();
        desempenhoDeFechamento();
        campoSeparadoNaoContaCidadeDaMesmaPeca();

        System.out.println();
        System.out.println("  " + passou + " passaram, " + falhou + " falharam");
        return falhou == 0 ? 0 : 1;
    }



    // ---------- casos ----------

    private static void baralhoTem72Cartelas() {
        Baralho baralho = new Baralho();
        checar("baralho tem 72 cartelas (RF02.1)",
                baralho.quantidadeRestante() == Baralho.TOTAL_DE_CARTELAS,
                "tinha " + baralho.quantidadeRestante());
    }

    private static void rotacaoCompletaVoltaAoOriginal() {
        Peca peca = Baralho.criarPecaInicial();
        String antes = bordas(peca);
        for (int i = 0; i < 4; i++) {
            peca.rotacionarDireita();
        }
        checar("4 rotacoes de 90 graus voltam ao estado inicial (RF02.4)",
                bordas(peca).equals(antes) && peca.getRotacao() == 0,
                antes + " -> " + bordas(peca));
    }

    private static void rotacaoGiraAsBordas() {
        Peca peca = Baralho.criarPecaInicial(); // CastleWallRoad: N=cidade, L=estrada, S=campo, O=estrada
        TipoTerreno norteAntes = peca.bordaEm(Lado.NORTE);
        peca.rotacionarDireita();
        checar("girar a direita leva o norte para o leste",
                peca.bordaEm(Lado.LESTE) == norteAntes,
                "norte era " + norteAntes + ", leste virou " + peca.bordaEm(Lado.LESTE));

        peca.rotacionarEsquerda();
        checar("girar a esquerda desfaz o giro",
                peca.bordaEm(Lado.NORTE) == norteAntes, bordas(peca));
    }

    private static void encaixeComparaTerrenos() {
        Peca a = Baralho.criarPecaInicial(); // norte = cidade
        Peca b = Baralho.criarPecaInicial();
        // b ao norte de a: lado sul de b encosta no norte de a
        checar("cidade nao encaixa com campo (RF03.4)",
                !a.encaixaCom(b, Lado.NORTE),
                a.bordaEm(Lado.NORTE) + " vs " + b.bordaEm(Lado.SUL));

        b.rotacionarDireita();
        b.rotacionarDireita();
        checar("cidade encaixa com cidade depois de girar (RF03.4)",
                a.encaixaCom(b, Lado.NORTE),
                a.bordaEm(Lado.NORTE) + " vs " + b.bordaEm(Lado.SUL));
    }

    private static void segmentosDerivadosCorretamente() {
        Peca inicial = Baralho.criarPecaInicial();
        Segmento estrada = inicial.segmentoEm(Lado.LESTE);
        checar("a estrada reta da cartela inicial liga leste e oeste",
                estrada != null && estrada.getTipo() == TipoTerreno.ESTRADA
                        && estrada.quantidadeDeLados() == 2 && estrada.ocupa(Lado.OESTE),
                String.valueOf(estrada));

        Peca mosteiro = acharNoBaralho("Monastery#1");
        checar("cartela de mosteiro tem segmento de mosteiro sem lados",
                mosteiro.temMosteiro() && mosteiro.getSegmentos().stream()
                        .anyMatch(s -> s.getTipo() == TipoTerreno.MOSTEIRO && s.quantidadeDeLados() == 0),
                String.valueOf(mosteiro.getSegmentos().size()));

        Peca cruzamento = acharNoBaralho("RoadJunctionSmall#1");
        long estradas = cruzamento.getSegmentos().stream()
                .filter(s -> s.getTipo() == TipoTerreno.ESTRADA).count();
        checar("cruzamento parte a estrada em 3 segmentos",
                estradas == 3, "eram " + estradas);

        Peca cidadeOposta = acharNoBaralho("CastleSides#1");
        long cidades = cidadeOposta.getSegmentos().stream()
                .filter(s -> s.getTipo() == TipoTerreno.CIDADE).count();
        checar("cidade de lados opostos fica em 2 segmentos separados",
                cidades == 2, "eram " + cidades);

        Peca cidadeCanto = acharNoBaralho("CastleEdge#1");
        long cantos = cidadeCanto.getSegmentos().stream()
                .filter(s -> s.getTipo() == TipoTerreno.CIDADE).count();
        checar("cidade de canto une os dois lados em 1 segmento",
                cantos == 1, "eram " + cantos);
    }

    private static void segmentosMantemPosicaoLocalNaPeca() {
        Peca cruzamento = acharNoBaralho("RoadJunctionLarge#1");
        List<Segmento> estradas = cruzamento.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.ESTRADA)
                .toList();
        checar("cruzamento expõe uma posição local por estrada separada",
                estradas.size() == 4
                        && estradas.stream().map(Segmento::getPosicaoX).distinct().count() == 3
                        && estradas.stream().map(Segmento::getPosicaoY).distinct().count() == 3,
                estradas.stream().map(segmento -> segmento.getPosicaoX() + "," + segmento.getPosicaoY())
                        .toList().toString());

        Segmento norte = estradas.stream().filter(segmento -> segmento.ocupa(Lado.NORTE))
                .findFirst().orElseThrow();
        double x = norte.getPosicaoX();
        double y = norte.getPosicaoY();
        cruzamento.rotacionarDireita();
        checar("posição local do meeple permanece alinhada à rotação visual da peça",
                norte.ocupa(Lado.LESTE) && norte.getPosicaoX() == x && norte.getPosicaoY() == y,
                norte.getLados() + " em " + norte.getPosicaoX() + "," + norte.getPosicaoY());
    }

    private static void meepleSoPodeFicarNasBordasOuNoMosteiro() {
        Peca campoCentral = new Peca("CampoCentral", "campo.png", false, false);
        Segmento campo = new Segmento(TipoTerreno.CAMPO, List.of());
        campoCentral.adicionarSegmento(campo);
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocar(campoCentral, new Posicao(0, 0));
        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Jogada jogadaCampo = new Jogada(jogador, campoCentral, new Posicao(0, 0), 0);
        jogadaCampo.setSegmentoEscolhido(campo);

        Peca pecaMosteiro = acharNoBaralho("Monastery#1");
        Segmento mosteiro = pecaMosteiro.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.MOSTEIRO)
                .findFirst().orElseThrow();
        Tabuleiro tabuleiroMosteiro = new Tabuleiro();
        tabuleiroMosteiro.colocar(pecaMosteiro, new Posicao(0, 0));
        Jogada jogadaMosteiro = new Jogada(jogador, pecaMosteiro, new Posicao(0, 0), 0);
        jogadaMosteiro.setSegmentoEscolhido(mosteiro);

        checar("campo sem lado não oferece nem aceita meeple",
                !jogadaCampo.temOpcaoDeSeguidor() && !jogadaCampo.colocarSeguidor(),
                "aceitou meeple em campo sem lado");
        checar("mosteiro permite meeple no centro",
                tabuleiroMosteiro.pecaEm(new Posicao(0, 0)) == pecaMosteiro
                        && mosteiro.getLados().isEmpty() && jogadaMosteiro.colocarSeguidor(),
                "mosteiro recusou meeple central");

        Peca estradaReta = acharNoBaralho("Road#1");
        Segmento estrada = estradaReta.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.ESTRADA)
                .findFirst().orElseThrow();
        checar("estrada reta posiciona meeple fora do centro da peça",
                estrada.getPosicaoX() != 0.5 || estrada.getPosicaoY() != 0.5,
                estrada.getPosicaoX() + "," + estrada.getPosicaoY());
    }

    private static void camposEmCurvasFicamSeparadosPelasEstradas() {
        Peca curva = acharNoBaralho("RoadCurve#1");
        List<Segmento> campos = curva.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.CAMPO)
                .toList();
        Segmento campo = campos.stream()
                .filter(segmento -> segmento.ocupa(Lado.NORTE))
                .findFirst().orElse(null);

        checar("campo da curva fica acessível pelos lados sem atravessar a estrada",
                campos.size() == 2 && campo != null
                && campo.ocupa(Lado.LESTE)
                && !campo.ocupa(Lado.SUL)
                && !campo.ocupa(Lado.OESTE)
                && (campo.getPosicaoX() != 0.5 || campo.getPosicaoY() != 0.5),
                campos.stream().map(segmento -> segmento.getLados().toString()).toList().toString());

        Segmento campoNorte = curva.segmentoEm(Canto.NOROESTE);
        Segmento campoSudoeste = curva.segmentoEm(Canto.SUDOESTE);
        checar("quina sem lado de campo forma um segmento de campo proprio",
                campoNorte != null && campoSudoeste != null && campoNorte != campoSudoeste
                        && campoSudoeste.getLados().isEmpty(),
                String.valueOf(campoSudoeste));
        curva.rotacionarDireita();
        checar("quinas dos campos giram junto com a cartela",
                campoNorte.ocupa(Canto.NORDESTE)
                        && campoNorte.ocupa(Lado.LESTE),
                campoNorte.getCantos() + " / " + campoNorte.getLados());
    }

    private static void camposQueSeEncontramEmQuinasSaoUnificados() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca primeiraCurva = acharNoBaralho("RoadCurve#1");
        Posicao primeiraPosicao = new Posicao(0, 0);
        tabuleiro.colocar(primeiraCurva, primeiraPosicao);

        Peca estradaReta = acharNoBaralho("Road#1");
        Posicao estradaPosicao = new Posicao(1, 0);
        checar("estrada conecta as duas peças usadas no teste de quina",
                tabuleiro.colocar(estradaReta, estradaPosicao),
                "estrada não foi colocada");

        Peca segundaCurva = acharNoBaralho("RoadCurve#2");
        segundaCurva.rotacionarDireita();
        segundaCurva.rotacionarDireita();
        Posicao segundaPosicao = new Posicao(1, 1);
        checar("segunda curva encaixa abaixo da estrada",
                tabuleiro.colocar(segundaCurva, segundaPosicao),
                "segunda curva não foi colocada");

        Segmento campoPrimeiro = primeiraCurva.segmentoEm(Canto.SUDESTE);
        Segmento campoSegundo = segundaCurva.segmentoEm(Canto.NOROESTE);
        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Meeple fazendeiro = jogador.retirarMeeple();
        boolean alocado = fazendeiro.ocupar(campoPrimeiro);
        Jogada jogada = new Jogada(jogador, segundaCurva, segundaPosicao, segundaCurva.getRotacao());
        jogada.setSegmentoEscolhido(campoSegundo);

        checar("campos unidos pela borda de estrada impedem um segundo fazendeiro",
                campoPrimeiro.getRegiao() == campoSegundo.getRegiao()
                        && alocado && !jogada.colocarSeguidor()
                        && jogador.getMeeplesDisponiveis() == Jogador.MEEPLES_INICIAIS - 1
                        && campoPrimeiro.getRegiao().getMeeples().size() == 1,
                "mesmo campo=" + (campoPrimeiro.getRegiao() == campoSegundo.getRegiao())
                        + ", meeples=" + campoPrimeiro.getRegiao().getMeeples().size());
    }

    /**
     * Estrada leste-oeste continuando para leste: o campo ao sul da primeira
     * cartela encosta no campo ao sul da seguinte pela metade da borda de estrada.
     */
    private static void camposDosDoisLadosDaEstradaAtravessamABorda() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca inicial = Baralho.criarPecaInicial();
        tabuleiro.colocar(inicial, new Posicao(0, 0));
        Peca primeira = acharNoBaralho("Road#1");
        primeira.rotacionarDireita();
        Posicao primeiraPosicao = new Posicao(1, 0);
        tabuleiro.colocar(primeira, primeiraPosicao);
        Peca segunda = acharNoBaralho("Road#2");
        segunda.rotacionarDireita();
        Posicao segundaPosicao = new Posicao(2, 0);
        tabuleiro.colocar(segunda, segundaPosicao);

        Segmento campoInicial = inicial.segmentoEm(Lado.SUL);
        Segmento campoPrimeira = primeira.segmentoEm(Lado.SUL);
        Segmento campoSegunda = segunda.segmentoEm(Lado.SUL);
        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Jogada primeiraJogada = new Jogada(jogador, primeira, primeiraPosicao, primeira.getRotacao());
        primeiraJogada.setSegmentoEscolhido(campoPrimeira);
        boolean alocado = primeiraJogada.colocarSeguidor();
        Jogada segundaJogada = new Jogada(jogador, segunda, segundaPosicao, segunda.getRotacao());
        segundaJogada.setSegmentoEscolhido(campoSegunda);

        checar("campo atravessa a borda de estrada e bloqueia segundo fazendeiro (RF04.3)",
                campoInicial.getRegiao() == campoPrimeira.getRegiao()
                        && campoPrimeira.getRegiao() == campoSegunda.getRegiao()
                        && alocado && !segundaJogada.colocarSeguidor()
                        && campoPrimeira.getRegiao().getMeeples().size() == 1,
                "inicial=primeira: " + (campoInicial.getRegiao() == campoPrimeira.getRegiao())
                        + ", primeira=segunda: " + (campoPrimeira.getRegiao() == campoSegunda.getRegiao()));
    }

    /**
     * Duas muralhas que so se tocam pela quina, com a cidade da cartela entre
     * elas ocupando aquele canto: os campos continuam separados.
     */
    private static void camposSoNaDiagonalNaoSeUnem() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca cidadeNoCanto = acharNoBaralho("CastleEdge#1");
        cidadeNoCanto.rotacionarDireita(); // cidade a leste e ao sul
        tabuleiro.colocar(cidadeNoCanto, new Posicao(0, -1));
        Peca muralhaAoNorte = acharNoBaralho("CastleWall#1");
        tabuleiro.colocar(muralhaAoNorte, new Posicao(0, 0));
        Peca muralhaAOeste = acharNoBaralho("CastleWall#2");
        muralhaAOeste.rotacionarEsquerda();
        tabuleiro.colocar(muralhaAOeste, new Posicao(1, -1));

        Segmento campoAbaixo = muralhaAoNorte.segmentoEm(Canto.NORDESTE);
        Segmento campoAoLado = muralhaAOeste.segmentoEm(Canto.SUDOESTE);
        checar("campos que so se tocam na diagonal ficam separados",
                campoAbaixo.getRegiao() != campoAoLado.getRegiao(),
                "os dois campos viraram a mesma regiao");
    }

    /**
     * Nas cartelas com muralha e estrada, a faixa de campo entre a cidade e a
     * estrada atravessa a cartela inteira. Tabuleiro montado a partir de uma
     * partida real em que tres pares de fazendeiros dividiam o mesmo campo.
     */
    private static void faixaDeCampoEntreCidadeEEstradaEUmCampoSo() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocar(Baralho.criarPecaInicial(), new Posicao(0, 0));
        Peca cruzamento = colocarNoTabuleiro(tabuleiro, "RoadJunctionSmall#1", 0, -1, 0);
        Peca curva = colocarNoTabuleiro(tabuleiro, "RoadCurve#1", 0, 1, 0);
        Peca estradaOeste = colocarNoTabuleiro(tabuleiro, "Road#1", 0, -1, 1);
        Peca estradaLeste = colocarNoTabuleiro(tabuleiro, "Road#2", 0, 1, 1);
        colocarNoTabuleiro(tabuleiro, "CastleWallCurveLeft#1", 2, -1, 2);
        Peca muralhaComCurva = colocarNoTabuleiro(tabuleiro, "CastleWallCurveLeft#2", 0, 0, 2);
        Peca estradaSul = colocarNoTabuleiro(tabuleiro, "Road#3", 0, 0, 3);

        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Segmento[][] pares = {
            {cruzamento.segmentoEm(Lado.NORTE), curva.segmentoEm(Lado.NORTE)},
            {estradaLeste.segmentoEm(Lado.OESTE), muralhaComCurva.segmentoEm(Lado.LESTE)},
            {estradaOeste.segmentoEm(Lado.OESTE), estradaSul.segmentoEm(Lado.OESTE)}
        };
        boolean todosBloqueados = true;
        StringBuilder detalhe = new StringBuilder();
        for (Segmento[] par : pares) {
            boolean alocado = jogador.retirarMeeple().ocupar(par[0]);
            todosBloqueados &= alocado && par[1].estaOcupado();
            detalhe.append(par[1].estaOcupado()).append(" ");
        }
        checar("faixa entre cidade e estrada liga os campos e bloqueia o segundo fazendeiro",
                todosBloqueados, "segundo campo ocupado por par: " + detalhe.toString().trim());
        checar("os tres campos do tabuleiro continuam distintos entre si",
                pares[0][0].getRegiao() != pares[1][0].getRegiao()
                        && pares[1][0].getRegiao() != pares[2][0].getRegiao()
                        && pares[0][0].getRegiao() != pares[2][0].getRegiao(),
                "campos foram fundidos demais");
    }

    private static void naoColocaIsoladaNemSobrepoe() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocar(Baralho.criarPecaInicial(), new Posicao(0, 0));

        Peca outra = Baralho.criarPecaInicial();
        checar("posicao isolada e recusada (RF03.3)",
                !tabuleiro.podeColocar(outra, new Posicao(5, 5)), "aceitou (5,5)");
        checar("posicao ocupada e recusada (RF03.3)",
                !tabuleiro.podeColocar(outra, new Posicao(0, 0)), "aceitou (0,0)");
    }

    private static void candidatasDependemDaRotacao() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocar(Baralho.criarPecaInicial(), new Posicao(0, 0));

        Peca peca = acharNoBaralho("Road#1"); // estrada reta norte-sul
        List<Posicao> antes = tabuleiro.posicoesCandidatas(peca);
        peca.rotacionarDireita();
        List<Posicao> depois = tabuleiro.posicoesCandidatas(peca);

        checar("com 1 cartela no tabuleiro ha no maximo 4 candidatas",
                antes.size() <= 4 && depois.size() <= 4,
                antes.size() + " / " + depois.size());
        checar("girar a cartela muda as posicoes validas (RNF04.1)",
                !antes.equals(depois), antes + " vs " + depois);
    }

    private static void estradaFechaNasDuasPontas() {
        Tabuleiro tabuleiro = new Tabuleiro();
        // a cartela inicial tem estrada ligando leste e oeste
        tabuleiro.colocar(Baralho.criarPecaInicial(), new Posicao(0, 0));

        Regiao estrada = tabuleiro.getRegioes().stream()
                .filter(r -> r.getTipo() == TipoTerreno.ESTRADA)
                .findFirst().orElseThrow();
        checar("estrada da cartela inicial nasce com 2 pontas abertas",
                estrada.getPontasAbertas() == 2, "pontas=" + estrada.getPontasAbertas());

        // MonasteryRoad e o beco sem saida: um unico lado de estrada
        checar("beco sem saida fecha a ponta leste",
                encaixarGirando(tabuleiro, acharNoBaralho("MonasteryRoad#1"), new Posicao(1, 0)),
                "nao coube a leste");
        checar("com uma ponta fechada a estrada ainda nao esta completa",
                !estrada.estaCompleta(), "pontas=" + estrada.getPontasAbertas());

        checar("beco sem saida fecha a ponta oeste",
                encaixarGirando(tabuleiro, acharNoBaralho("MonasteryRoad#2"), new Posicao(-1, 0)),
                "nao coube a oeste");
        checar("estrada fechada nas duas pontas esta completa (RF05.1)",
                estrada.estaCompleta(), "pontas=" + estrada.getPontasAbertas());
        checar("estrada de 3 cartelas vale 3 pontos (RF05.3)",
                estrada.calcularPontos(false) == 3, "valeu " + estrada.calcularPontos(false));
    }

    private static void meeplePontuaEVoltaAReserva() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca inicial = Baralho.criarPecaInicial();
        Posicao posicaoInicial = new Posicao(0, 0);
        tabuleiro.colocar(inicial, posicaoInicial);
        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Jogada jogada = new Jogada(jogador, inicial, posicaoInicial, inicial.getRotacao());
        Segmento estrada = inicial.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.ESTRADA)
                .findFirst().orElseThrow();

        jogada.setSegmentoEscolhido(estrada);
        boolean alocado = jogada.colocarSeguidor();
        Meeple meeple = jogada.getMeepleColocado();
        checar("meeple alocado na estrada assume papel de ladrão e sai da reserva",
                alocado && meeple.getTipo() == TipoMeeple.LADRAO
                        && jogador.getMeeplesDisponiveis() == 6,
                "alocado=" + alocado + ", papel=" + (meeple == null ? null : meeple.getTipo()));
        jogada.setSegmentoEscolhido(estrada);
        checar("região ocupada recusa um segundo meeple",
                !jogada.colocarSeguidor() && jogador.getMeeplesDisponiveis() == 6,
                "reserva=" + jogador.getMeeplesDisponiveis());

        encaixarGirando(tabuleiro, acharNoBaralho("MonasteryRoad#1"), new Posicao(1, 0));
        encaixarGirando(tabuleiro, acharNoBaralho("MonasteryRoad#2"), new Posicao(-1, 0));
        int pontosDistribuidos = jogada.pontuarRegioesConcluidas(tabuleiro);
        checar("estrada concluída pontua e devolve meeple à reserva",
                pontosDistribuidos == 3 && jogador.getPontos() == 3
                        && jogador.getMeeplesDisponiveis() == Jogador.MEEPLES_INICIAIS
                        && estrada.getRegiao().getMeeples().isEmpty(),
                "pontos=" + jogador.getPontos() + ", reserva=" + jogador.getMeeplesDisponiveis());
    }

    private static void jogadaSemRegiaoLivreNaoOfereceMeeple() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca cidade = acharNoBaralho("CastleCenter#1");
        Posicao posicao = new Posicao(0, 0);
        tabuleiro.colocar(cidade, posicao);
        Jogador jogador = new Jogador("Teste", Cor.VERMELHO, new EstrategiaHumana());
        Jogada jogada = new Jogada(jogador, cidade, posicao, cidade.getRotacao());

        Segmento cidadeOcupada = cidade.getSegmentos().get(0);
        Jogador donoMeeple = new Jogador("Dono", Cor.AZUL, new EstrategiaHumana());
        donoMeeple.retirarMeeple().ocupar(cidadeOcupada);

        checar("jogada sem região livre não oferece alocação de meeple",
                !jogada.temOpcaoDeSeguidor(),
                "regiao ocupada=" + cidadeOcupada.getRegiao().estaOcupada());
    }

    private static void fazendeiroNaoPodeDuplicarNaRegiaoConectada() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca primeira = acharNoBaralho("Monastery#1");
        tabuleiro.colocar(primeira, new Posicao(0, 0));
        Segmento campoInicial = primeira.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.CAMPO)
                .findFirst().orElseThrow();

        Jogador primeiroJogador = new Jogador("Primeiro", Cor.VERMELHO, new EstrategiaHumana());
        Meeple primeiroFazendeiro = primeiroJogador.retirarMeeple();
        boolean primeiroAlocado = primeiroFazendeiro.ocupar(campoInicial);

        Peca segunda = acharNoBaralho("Monastery#2");
        Posicao posicaoVizinha = new Posicao(1, 0);
        boolean colocouPeca = tabuleiro.colocar(segunda, posicaoVizinha);
        Segmento campoConectado = segunda.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.CAMPO)
                .findFirst().orElseThrow();
        Jogador segundoJogador = new Jogador("Segundo", Cor.AZUL, new EstrategiaHumana());
        Jogada jogada = new Jogada(segundoJogador, segunda, posicaoVizinha, segunda.getRotacao());
        jogada.setSegmentoEscolhido(campoConectado);

        checar("fazendeiro existente bloqueia outro na regiao de campo conectada",
                primeiroAlocado && colocouPeca
                        && campoConectado.getRegiao() == campoInicial.getRegiao()
                        && !jogada.colocarSeguidor()
                        && segundoJogador.getMeeplesDisponiveis() == Jogador.MEEPLES_INICIAIS
                        && campoInicial.getRegiao().getMeeples().size() == 1,
                "meeples na regiao=" + campoInicial.getRegiao().getMeeples().size()
                        + ", reserva do segundo=" + segundoJogador.getMeeplesDisponiveis());
    }

    private static void mosteiroFechaComOitoVizinhas() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca mosteiro = acharNoBaralho("Monastery#1");
        tabuleiro.colocar(mosteiro, new Posicao(0, 0));

        Mosteiro regiao = (Mosteiro) mosteiro.getSegmentos().stream()
                .filter(s -> s.getTipo() == TipoTerreno.MOSTEIRO)
                .findFirst().orElseThrow().getRegiao();
        checar("mosteiro sozinho nao esta completo", !regiao.estaCompleta(), "completo cedo demais");

        // primeiro as 4 ortogonais, depois as 4 diagonais: uma diagonal colocada
        // cedo demais nao teria vizinha ortogonal e seria recusada (RF03.3)
        int[][] ordem = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        int n = 1;
        for (int[] passo : ordem) {
            Peca vizinha = acharNoBaralho("Monastery#" + (n % 4 + 1));
            boolean colocou = tabuleiro.colocar(vizinha, new Posicao(passo[0], passo[1]));
            checar("vizinha (" + passo[0] + "," + passo[1] + ") entra no tabuleiro",
                    colocou, "recusada");
            n++;
        }

        checar("mosteiro cercado por 8 cartelas fecha (RF05.1)",
                regiao.estaCompleta(), "vizinhas=" + regiao.getCartelasVizinhas());
        checar("mosteiro fechado vale 9 pontos (RF05.3)",
                regiao.calcularPontos(false) == 9, "valeu " + regiao.calcularPontos(false));
    }

    private static void cidadeContaBrasao() {
        Peca comBrasao = acharNoBaralho("CastleCenter#1");
        checar("cartela quase toda cidade tem brasao", comBrasao.temBrasao(), "sem brasao");
        Peca semBrasao = acharNoBaralho("Road#1");
        checar("cartela de estrada nao tem brasao", !semBrasao.temBrasao(), "com brasao");
    }

    private static void descarteQuandoNaoCabe() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida("Teste", "ai");
        checar("partida nova ja tem cartela na mao",
                controlador.getPecaAtual() != null, "mao vazia");
        checar("partida nova tem posicoes validas (RNF04.1)",
                !controlador.candidatas().isEmpty(), "nenhuma candidata");

        int restantesAntes = controlador.getPartida().getBaralho().quantidadeRestante();
        Posicao alvo = controlador.candidatas().get(0);
        boolean ok = controlador.colocar(alvo.getX(), alvo.getY());
        checar("colocar numa candidata aguarda escolha quando ha meeple e regiao livre",
                ok && controlador.getJogadaPendente() != null
                        && controlador.getPecaAtual() == null
                        && controlador.getPartida().getBaralho().quantidadeRestante() == restantesAntes,
                "ok=" + ok);
        boolean concluiu = controlador.concluirJogada(-1);
        checar("pular a alocacao conclui a jogada e compra a proxima peca",
                concluiu && controlador.getJogadaPendente() == null
                        && controlador.getPecaAtual() != null
                        && controlador.getPartida().getBaralho().quantidadeRestante() < restantesAntes,
                "ok=" + ok);
        checar("colocar em posicao invalida e recusado",
                !controlador.colocar(99, 99), "aceitou (99,99)");
    }

    private static void semMeeplesContinuaAutomaticamente() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida("Reserva vazia", "ai");
        for (int i = 0; i < Jogador.MEEPLES_INICIAIS; i++) {
            controlador.getJogador().retirarMeeple();
        }

        int restantesAntes = controlador.getPartida().getBaralho().quantidadeRestante();
        Posicao alvo = controlador.candidatas().get(0);
        boolean colocou = controlador.colocar(alvo.getX(), alvo.getY());
        checar("sem meeples a jogada continua automaticamente",
                colocou && controlador.getJogadaPendente() == null
                        && controlador.getPecaAtual() != null
                        && controlador.getPartida().getBaralho().quantidadeRestante() < restantesAntes,
                "colocou=" + colocou + ", pendente=" + (controlador.getJogadaPendente() != null));
    }

    private static void turnosPassamPelosTresJogadores() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida(List.of("Ana", "Bia", "Caio"));
        boolean iniciouComPrimeira = controlador.getJogador().getNome().equals("Ana");

        concluirTurnoSemMeeple(controlador);
        boolean passouParaSegunda = controlador.getJogador().getNome().equals("Bia");
        concluirTurnoSemMeeple(controlador);
        boolean passouParaTerceiro = controlador.getJogador().getNome().equals("Caio");
        concluirTurnoSemMeeple(controlador);

        checar("turnos passam pelos tres jogadores e voltam ao primeiro",
                iniciouComPrimeira && passouParaSegunda && passouParaTerceiro
                        && controlador.getJogador().getNome().equals("Ana")
                        && controlador.getPartida().getRodadas().size() == 2,
                "jogador atual=" + controlador.getJogador().getNome()
                        + ", rodadas=" + controlador.getPartida().getRodadas().size());
    }

    private static void iaExecutaOTurnoSemInteracao() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida("Ana", "ai");
        boolean configurouOponente = controlador.getPartida().getJogadores().size() == 2
                && controlador.getPartida().getJogadores().get(1).getEstrategia() instanceof EstrategiaIA;
        int pecasAntes = controlador.getPartida().getTabuleiro().quantidadeDePecas();
        long inicioDecisao = System.nanoTime();
        Posicao candidata = controlador.candidatas().get(0);
        boolean colocou = controlador.colocar(candidata.getX(), candidata.getY());
        if (controlador.getJogadaPendente() != null) {
            controlador.concluirJogada(-1);
        }
        long tempoDecisaoMs = (System.nanoTime() - inicioDecisao) / 1_000_000;
        int pecasDepois = controlador.getPartida().getTabuleiro().quantidadeDePecas();
        checar("modo contra IA cria um oponente automatizado",
                configurouOponente, controlador.getPartida().getJogadores().toString());
        checar("IA posiciona a peça e conclui a vez sem interação humana",
                colocou && pecasDepois == pecasAntes + 2
                        && controlador.getJogador().getNome().equals("Ana")
                        && controlador.getJogadaPendente() == null
                        && controlador.getPecaAtual() != null,
                "peças antes/depois=" + pecasAntes + "/" + pecasDepois
                        + ", jogador atual=" + controlador.getJogador().getNome());
        checar("IA decide e executa em menos de 2 segundos (RNF05.1)",
                tempoDecisaoMs < 2_000, tempoDecisaoMs + "ms");
    }

    private static void partidaConfiguraMultiplosHumanosEIAs() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida(List.of("Ana", "Bia"), 2);
        List<Jogador> jogadores = controlador.getPartida().getJogadores();
        boolean iasIndependentes = jogadores.size() == 4
                && jogadores.get(2).getEstrategia() instanceof EstrategiaIA
                && jogadores.get(3).getEstrategia() instanceof EstrategiaIA
                && jogadores.get(2).getEstrategia() != jogadores.get(3).getEstrategia();

        int pecasAntes = controlador.getPartida().getTabuleiro().quantidadeDePecas();
        concluirTurnoSemMeeple(controlador);
        concluirTurnoSemMeeple(controlador);
        boolean iasJogaramAutomaticamente = controlador.getPartida().getTabuleiro().quantidadeDePecas()
                == pecasAntes + 4 && controlador.getJogador().getNome().equals("Ana");

        ControladorDePartida maximo = new ControladorDePartida();
        maximo.novaPartida(List.of("Ana", "Bia", "Caio", "Dora"), 1);
        boolean limitePermitido = maximo.getPartida().getJogadores().size() == 5;

        boolean rejeitouExcesso = false;
        try {
            maximo.novaPartida(List.of("Ana", "Bia", "Caio", "Dora"), 2);
        } catch (IllegalArgumentException entradaInvalida) {
            rejeitouExcesso = true;
        }

        checar("cada IA possui estrategia independente",
                iasIndependentes, jogadores.toString());
        checar("multiplas IAs executam suas jogadas sem interacao",
                iasJogaramAutomaticamente,
                "pecas=" + controlador.getPartida().getTabuleiro().quantidadeDePecas());
        checar("partida aceita quatro humanos e uma IA",
                limitePermitido, "participantes=" + maximo.getPartida().getJogadores().size());
        checar("partida rejeita mais de cinco participantes",
                rejeitouExcesso, "aceitou seis participantes");
    }

    private static void concluirTurnoSemMeeple(ControladorDePartida controlador) {
        Posicao candidata = controlador.candidatas().get(0);
        controlador.colocar(candidata.getX(), candidata.getY());
        if (controlador.getJogadaPendente() != null) {
            controlador.concluirJogada(-1);
        }
    }

    /**
     * Cenario deterministico do RF02.3: um tabuleiro so com mosteiro expoe
     * apenas campo, entao a cartela toda de cidade nao cabe em rotacao alguma.
     */
    private static void cartelaSemEncaixeEmLugarNenhum() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocar(acharNoBaralho("Monastery#1"), new Posicao(0, 0));

        Peca sohCidade = acharNoBaralho("CastleCenter#1");
        boolean coubeEmAlguma = false;
        for (int giro = 0; giro < 4; giro++) {
            coubeEmAlguma |= !tabuleiro.posicoesCandidatas(sohCidade).isEmpty();
            sohCidade.rotacionarDireita();
        }
        checar("cartela toda de cidade nao encaixa ao redor de um mosteiro (RF02.3)",
                !coubeEmAlguma, "encontrou encaixe");

        Peca soCampo = acharNoBaralho("Monastery#2");
        checar("cartela de campo encaixa ao redor do mosteiro",
                !tabuleiro.posicoesCandidatas(soCampo).isEmpty(), "nao coube");
    }

    /** Nenhuma cartela some: colocadas + descartadas + restantes = 72. */
    private static void nenhumaCartelaSeperde() {
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida("Conservacao", "ai");
        while (!controlador.encerrada()) {
            if (controlador.getJogadaPendente() != null) {
                controlador.concluirJogada(-1);
                continue;
            }
            List<Posicao> candidatas = controlador.candidatas();
            if (candidatas.isEmpty()) {
                break;
            }
            controlador.colocar(candidatas.get(0).getX(), candidatas.get(0).getY());
        }
        int noTabuleiro = controlador.getPartida().getTabuleiro().quantidadeDePecas() - 1; // menos a inicial
        int descartadas = controlador.getPartida().getBaralho().quantidadeDescartada();
        int restantes = controlador.getPartida().getBaralho().quantidadeRestante();
        checar("colocadas + descartadas + restantes = 72 (RF02.1)",
                noTabuleiro + descartadas + restantes == Baralho.TOTAL_DE_CARTELAS,
                noTabuleiro + " + " + descartadas + " + " + restantes);
        checar("a partida termina com o baralho vazio (RF01.5)",
                restantes == 0, "sobraram " + restantes);
    }

    private static void desempenhoDeFechamento() {
        long inicio = System.nanoTime();
        ControladorDePartida controlador = new ControladorDePartida();
        controlador.novaPartida("Desempenho", "ai");
        int jogadas = 0;
        while (!controlador.encerrada() && jogadas < 72) {
            if (controlador.getJogadaPendente() != null) {
                controlador.concluirJogada(-1);
                continue;
            }
            List<Posicao> candidatas = controlador.candidatas();
            if (candidatas.isEmpty()) {
                break;
            }
            controlador.colocar(candidatas.get(0).getX(), candidatas.get(0).getY());
            jogadas++;
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        checar("partida inteira roda em menos de 100ms por jogada (RNF02.2)",
                ms < 100 * Math.max(1, jogadas),
                ms + "ms em " + jogadas + " jogadas");
        System.out.println("      (" + jogadas + " cartelas colocadas em " + ms + "ms)");
    }

    // ---------- apoio ----------

    /** Tenta as 4 rotacoes ate a cartela caber na posicao. */
    private static boolean encaixarGirando(Tabuleiro tabuleiro, Peca peca, Posicao posicao) {
        for (int giro = 0; giro < 4; giro++) {
            if (tabuleiro.colocar(peca, posicao)) {
                return true;
            }
            peca.rotacionarDireita();
        }
        return false;
    }

    private static Peca acharNoBaralho(String id) {
        Baralho baralho = new Baralho();
        Peca peca;
        while ((peca = baralho.comprar()) != null) {
            if (peca.getId().equals(id)) {
                return peca;
            }
        }
        throw new IllegalArgumentException("cartela inexistente no catalogo: " + id);
    }

    private static Peca colocarNoTabuleiro(Tabuleiro tabuleiro, String id, int giros, int x, int y) {
        Peca peca = acharNoBaralho(id);
        for (int i = 0; i < giros; i++) {
            peca.rotacionarDireita();
        }
        if (!tabuleiro.colocar(peca, new Posicao(x, y))) {
            throw new IllegalStateException(id + " nao encaixa em (" + x + "," + y + ")");
        }
        return peca;
    }

    private static String bordas(Peca peca) {
        StringBuilder texto = new StringBuilder();
        for (Lado lado : Lado.values()) {
            texto.append(lado).append("=").append(peca.bordaEm(lado)).append(" ");
        }
        return texto.toString().trim();
    }

    private static void checar(String descricao, boolean condicao, String detalhe) {
        if (condicao) {
            passou++;
            System.out.println("  ok   " + descricao);
        } else {
            falhou++;
            System.out.println("  FALHA " + descricao + "  -> " + detalhe);
        }
    }

    /*talvez tenhamos problema na contagem de cidades adjacentes, verificar*/
    private static void campoSeparadoNaoContaCidadeDaMesmaPeca() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Peca peca = acharNoBaralho("CastleWallCurveLeft#1");
        boolean colocou = tabuleiro.colocar(peca, new Posicao(0, 0));
        Campo campoSudoeste =
                (Campo) peca.segmentoEm(Canto.SUDOESTE).getRegiao();
        Cidade cidade = peca.getSegmentos().stream()
                .filter(segmento -> segmento.getTipo() == TipoTerreno.CIDADE)
                .map(Segmento::getRegiao)
                .map(Cidade.class::cast)
                .findFirst()
                .orElseThrow();
        checar("campo sudoeste não conta cidade que não toca",
                colocou && !campoSudoeste.getCidadesAdjacentes().contains(cidade),
                "cidade registrada como adjacente ao campo sudoeste");
    }
}
