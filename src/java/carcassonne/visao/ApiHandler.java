package carcassonne.visao;

import carcassonne.controle.ControladorDePartida;
import carcassonne.modelo.Lado;
import carcassonne.modelo.Meeple;
import carcassonne.modelo.Peca;
import carcassonne.modelo.Posicao;
import carcassonne.modelo.Regiao;
import carcassonne.modelo.Segmento;
import carcassonne.modelo.TipoMeeple;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traduz o estado do modelo para JSON e devolve as acoes da tela ao controlador.
 * E o unico ponto onde o dominio encosta no mundo HTTP.
 */
public class ApiHandler implements HttpHandler {
    private final ControladorDePartida controlador = new ControladorDePartida();

    @Override
    public void handle(HttpExchange troca) throws IOException {
        try {
            String rota = troca.getRequestURI().getPath();
            String corpo = "POST".equals(troca.getRequestMethod())
                    ? ServidorWeb.lerCorpo(troca) : "";

            switch (rota) {
                case "/api/partida" -> {
                    String modo = Json.texto(corpo, "modo", "human");
                    if (!"human".equals(modo)) {
                        throw new IllegalArgumentException("Este modo de partida ainda não está disponível.");
                    }
                    controlador.novaPartida(Json.textos(corpo, "jogadores"));
                    responderEstado(troca);
                }
                case "/api/rotacionar" -> {
                    exigirPartida();
                    controlador.rotacionar(!"esquerda".equals(Json.texto(corpo, "sentido", "direita")));
                    responderEstado(troca);
                }
                case "/api/colocar" -> {
                    exigirPartida();
                    controlador.colocar(Json.inteiro(corpo, "x", 0), Json.inteiro(corpo, "y", 0));
                    responderEstado(troca);
                }
                case "/api/meeple" -> {
                    exigirPartida();
                    controlador.concluirJogada(Json.inteiro(corpo, "segmento", -1));
                    responderEstado(troca);
                }
                case "/api/estado" -> {
                    exigirPartida();
                    responderEstado(troca);
                }
                default -> erro(troca, 404, "rota desconhecida: " + rota);
            }
        } catch (IllegalStateException semPartida) {
            erro(troca, 409, semPartida.getMessage());
        } catch (IllegalArgumentException entradaInvalida) {
            erro(troca, 400, entradaInvalida.getMessage());
        } catch (RuntimeException falha) {
            erro(troca, 500, String.valueOf(falha.getMessage()));
        }
    }

    private void exigirPartida() {
        if (!controlador.temPartida()) {
            throw new IllegalStateException("nenhuma partida em andamento");
        }
    }

    private void responderEstado(HttpExchange troca) throws IOException {
        byte[] corpo = Json.escrever(montarEstado()).getBytes(StandardCharsets.UTF_8);
        ServidorWeb.responder(troca, 200, "application/json; charset=utf-8", corpo);
    }

    private void erro(HttpExchange troca, int status, String mensagem) throws IOException {
        byte[] corpo = Json.escrever(Map.of("erro", mensagem)).getBytes(StandardCharsets.UTF_8);
        ServidorWeb.responder(troca, status, "application/json; charset=utf-8", corpo);
    }

    /** Monta a fotografia do jogo que a tela precisa para desenhar tudo. */
    private Map<String, Object> montarEstado() {
        Map<String, Object> estado = new LinkedHashMap<>();

        estado.put("jogadores", controlador.getPartida().getJogadores().stream()
                .map(jogador -> Map.of(
                        "nome", jogador.getNome(),
                        "cor", jogador.getCor().name(),
                        "pontos", jogador.getPontos(),
                        "meeples", jogador.getMeeplesDisponiveis(),
                        "vez", jogador == controlador.getJogador()))
                .toList());
        estado.put("jogador", Map.of(
                "nome", controlador.getJogador().getNome(),
                "cor", controlador.getJogador().getCor().name(),
                "pontos", controlador.getJogador().getPontos(),
                "meeples", controlador.getJogador().getMeeplesDisponiveis()));

        estado.put("modo", controlador.getModo());

        estado.put("baralho", Map.of(
                "restantes", controlador.getPartida().getBaralho().quantidadeRestante(),
                "total", carcassonne.modelo.Baralho.TOTAL_DE_CARTELAS,
                "descartadas", controlador.getPartida().getBaralho().quantidadeDescartada()));

        estado.put("pecaAtual", descreverPecaAtual());
        estado.put("aguardandoMeeple", controlador.getJogadaPendente() != null);
        estado.put("segmentos", descreverSegmentosPendentes());
        estado.put("candidatas", descreverCandidatas());
        estado.put("tabuleiro", descreverTabuleiro());
        estado.put("encerrada", controlador.encerrada());
        estado.put("mensagem", controlador.getMensagem());
        return estado;
    }

    private Object descreverPecaAtual() {
        Peca peca = controlador.getPecaAtual();
        if (peca == null) {
            return null;
        }
        Map<String, Object> bordas = new LinkedHashMap<>();
        for (Lado lado : Lado.values()) {
            bordas.put(lado.name(), peca.bordaEm(lado) == null ? null : peca.bordaEm(lado).name());
        }
        Map<String, Object> descricao = new LinkedHashMap<>();
        descricao.put("id", peca.getId());
        descricao.put("imagem", peca.getImagem());
        descricao.put("rotacao", peca.getRotacao());
        descricao.put("brasao", peca.temBrasao());
        descricao.put("bordas", bordas);
        return descricao;
    }

    private List<Object> descreverCandidatas() {
        List<Object> lista = new ArrayList<>();
        for (Posicao posicao : controlador.candidatas()) {
            lista.add(Map.of("x", posicao.getX(), "y", posicao.getY()));
        }
        return lista;
    }

    private List<Object> descreverSegmentosPendentes() {
        List<Object> lista = new ArrayList<>();
        for (Segmento segmento : controlador.segmentosDaJogadaPendente()) {
            Regiao regiao = segmento.getRegiao();
            TipoMeeple papel = segmento.getTipo().papelDoMeeple();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("indice", segmento.getPeca().getSegmentos().indexOf(segmento));
            item.put("terreno", segmento.getTipo().name());
            item.put("papel", papel.name());
            item.put("ocupado", regiao.estaOcupada());
            item.put("lados", segmento.getLados().stream().map(Enum::name).toList());
            item.put("x", segmento.getPosicaoX());
            item.put("y", segmento.getPosicaoY());
            lista.add(item);
        }
        return lista;
    }

    private List<Object> descreverTabuleiro() {
        List<Object> lista = new ArrayList<>();
        controlador.getPartida().getTabuleiro().getPecas().forEach((posicao, peca) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("x", posicao.getX());
            item.put("y", posicao.getY());
            item.put("imagem", peca.getImagem());
            item.put("rotacao", peca.getRotacao());
            item.put("brasao", peca.temBrasao());
            item.put("inicial", peca.isInicial());
            item.put("meeples", descreverMeeples(peca));
            lista.add(item);
        });
        return lista;
    }

    private List<Object> descreverMeeples(Peca peca) {
        List<Object> lista = new ArrayList<>();
        Set<Meeple> incluidos = new HashSet<>();
        for (Segmento segmento : peca.getSegmentos()) {
            Regiao regiao = segmento.getRegiao();
            if (regiao == null) {
                continue;
            }
            for (Meeple meeple : regiao.getMeeples()) {
                if (meeple.getSegmento().getPeca() != peca || !incluidos.add(meeple)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("cor", meeple.getDono().getCor().name());
                item.put("nome", meeple.getDono().getNome());
                item.put("papel", meeple.getTipo().name());
                item.put("x", meeple.getSegmento().getPosicaoX());
                item.put("y", meeple.getSegmento().getPosicaoY());
                lista.add(item);
            }
        }
        return lista;
    }
}
