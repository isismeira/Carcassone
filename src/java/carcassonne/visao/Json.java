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

    /** Le uma lista JSON de textos, usada para receber os nomes dos jogadores. */
    public static List<String> textos(String json, String campo) {
        String marca = "\"" + campo + "\"";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo + ".");
        }
        int indice = json.indexOf(':', inicio + marca.length());
        if (indice < 0) {
            throw new IllegalArgumentException("Lista inválida no campo " + campo + ".");
        }
        indice = avancarEspacos(json, indice + 1);
        if (indice >= json.length() || json.charAt(indice) != '[') {
            throw new IllegalArgumentException("O campo " + campo + " precisa ser uma lista.");
        }

        List<String> valores = new java.util.ArrayList<>();
        indice = avancarEspacos(json, indice + 1);
        if (indice < json.length() && json.charAt(indice) == ']') {
            return valores;
        }

        while (indice < json.length()) {
            if (json.charAt(indice) != '"') {
                throw new IllegalArgumentException("A lista " + campo + " deve conter apenas textos.");
            }
            StringBuilder valor = new StringBuilder();
            indice++;
            boolean fechado = false;
            while (indice < json.length()) {
                char caractere = json.charAt(indice++);
                if (caractere == '"') {
                    fechado = true;
                    break;
                }
                if (caractere != '\\') {
                    valor.append(caractere);
                    continue;
                }
                if (indice >= json.length()) {
                    break;
                }
                char escape = json.charAt(indice++);
                switch (escape) {
                    case '"', '\\', '/' -> valor.append(escape);
                    case 'b' -> valor.append('\b');
                    case 'f' -> valor.append('\f');
                    case 'n' -> valor.append('\n');
                    case 'r' -> valor.append('\r');
                    case 't' -> valor.append('\t');
                    case 'u' -> {
                        if (indice + 4 > json.length()) {
                            throw new IllegalArgumentException("Escape Unicode inválido na lista " + campo + ".");
                        }
                        try {
                            valor.append((char) Integer.parseInt(json.substring(indice, indice + 4), 16));
                        } catch (NumberFormatException erro) {
                            throw new IllegalArgumentException("Escape Unicode inválido na lista " + campo + ".");
                        }
                        indice += 4;
                    }
                    default -> throw new IllegalArgumentException("Escape inválido na lista " + campo + ".");
                }
            }
            if (!fechado) {
                throw new IllegalArgumentException("Texto não terminado na lista " + campo + ".");
            }
            valores.add(valor.toString());
            indice = avancarEspacos(json, indice);
            if (indice >= json.length()) {
                break;
            }
            char separador = json.charAt(indice++);
            if (separador == ']') {
                return valores;
            }
            if (separador != ',') {
                throw new IllegalArgumentException("Separador inválido na lista " + campo + ".");
            }
            indice = avancarEspacos(json, indice);
        }
        throw new IllegalArgumentException("Lista não terminada no campo " + campo + ".");
    }

    private static int avancarEspacos(String texto, int indice) {
        while (indice < texto.length() && Character.isWhitespace(texto.charAt(indice))) {
            indice++;
        }
        return indice;
    }
}
