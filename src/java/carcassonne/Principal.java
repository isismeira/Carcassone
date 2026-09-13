package carcassonne;

import carcassonne.visao.ServidorWeb;

import java.util.Arrays;

/** Ponto de entrada: sobe o servidor ou roda a bateria de testes do modelo. */
public final class Principal {

    private Principal() {
    }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--testes")) {
            System.out.println("Conferindo as regras do modelo:");
            System.exit(Testes.executar());
        }
        int porta = 8080;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--porta")) {
                porta = Integer.parseInt(args[i + 1]);
            }
        }
        new ServidorWeb(porta).iniciar();
    }
}
