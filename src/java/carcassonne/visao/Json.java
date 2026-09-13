package carcassonne.visao;

import java.util.List;
import java.util.Map;

/**
 * Serializador minimo. Escreve o suficiente para o estado da partida e le os
 * poucos campos que a tela envia, evitando qualquer dependencia externa.
 */
public final class Json {

    private Json() {
    }

    /** Converte mapas, listas, textos, numeros e booleanos em JSON. */
    public static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(valor, saida);
        return saida.toString();
    }

    private static void escrever(Object valor, StringBuilder saida) {
        switch (valor) {
            case null -> saida.append("null");
            case String texto -> escreverTexto(texto, saida);
            case Boolean b -> saida.append(b);
            case Number n -> saida.append(n);
            case Map<?, ?> mapa -> {
                saida.append('{');
                boolean primeiro = true;
                for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                    if (!primeiro) {
                        saida.append(',');
                    }
                    primeiro = false;
                    escreverTexto(String.valueOf(entrada.getKey()), saida);
                    saida.append(':');
                    escrever(entrada.getValue(), saida);
                }
                saida.append('}');
            }
            case List<?> lista -> {
                saida.append('[');
                for (int i = 0; i < lista.size(); i++) {
                    if (i > 0) {
                        saida.append(',');
                    }
                    escrever(lista.get(i), saida);
                }
                saida.append(']');
            }
            default -> escreverTexto(String.valueOf(valor), saida);
        }
    }

    private static void escreverTexto(String texto, StringBuilder saida) {
        saida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20 || c > 0x7e) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        saida.append('"');
    }

    /**
     * Le um campo de texto de um JSON plano, sem montar arvore.
     * @return o valor encontrado, ou o padrao informado.
     */
    public static String texto(String json, String campo, String padrao) {
        String marca = "\"" + campo + "\"";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            return padrao;
        }
        int doisPontos = json.indexOf(':', inicio + marca.length());
        if (doisPontos < 0) {
            return padrao;
        }
        int aspas = json.indexOf('"', doisPontos);
        if (aspas < 0) {
            return padrao;
        }
        StringBuilder valor = new StringBuilder();
        for (int i = aspas + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char proximo = json.charAt(++i);
                valor.append(switch (proximo) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    default -> proximo;
                });
            } else if (c == '"') {
                return valor.toString();
            } else {
                valor.append(c);
            }
        }
        return padrao;
    }

    /** Le um campo numerico inteiro de um JSON plano. */
    public static int inteiro(String json, String campo, int padrao) {
        String marca = "\"" + campo + "\"";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            return padrao;
        }
        int doisPontos = json.indexOf(':', inicio + marca.length());
        if (doisPontos < 0) {
            return padrao;
        }
        StringBuilder numero = new StringBuilder();
        for (int i = doisPontos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || (c == '-' && numero.isEmpty())) {
                numero.append(c);
            } else if (!numero.isEmpty()) {
                break;
            } else if (c != ' ') {
                break;
            }
        }
        try {
            return Integer.parseInt(numero.toString());
        } catch (NumberFormatException erro) {
            return padrao;
        }
    }
}
