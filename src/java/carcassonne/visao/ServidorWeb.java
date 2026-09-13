package carcassonne.visao;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Camada de Visao: entrega os arquivos da interface e encaminha /api para o
 * {@link ApiHandler}. Usa o servidor embutido no JDK, sem dependencias externas.
 */
public class ServidorWeb {
    /** Onde ficam as paginas, o CSS e o JavaScript da interface. */
    private static final String PASTA_WEB = "src/web";

    /** Imagens; ficam fora de src/ por serem conteudo, nao codigo. */
    private static final String PASTA_ASSETS = "assets";

    private final int porta;
    private final Path raiz;

    public ServidorWeb(int porta) {
        this(porta, Path.of("").toAbsolutePath());
    }

    public ServidorWeb(int porta, Path raiz) {
        this.porta = porta;
        this.raiz = raiz;
    }

    public void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        ApiHandler api = new ApiHandler();
        servidor.createContext("/api", api);
        servidor.createContext("/", this::servirArquivo);
        servidor.setExecutor(Executors.newFixedThreadPool(4));
        servidor.start();
        System.out.println("Carcassonne no ar em http://localhost:" + porta);
        System.out.println("Interface em " + raiz.resolve(PASTA_WEB));
        System.out.println("Ctrl+C para encerrar.");
    }

    /**
     * As paginas vem de src/web e as imagens de assets. Os dois viram a mesma
     * arvore para o navegador: /jogo.html e /assets/tiles/Road0.png.
     */
    private void servirArquivo(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }
        Path base = caminho.startsWith("/" + PASTA_ASSETS + "/")
                ? raiz.resolve(PASTA_ASSETS)
                : raiz.resolve(PASTA_WEB);
        String relativo = caminho.startsWith("/" + PASTA_ASSETS + "/")
                ? caminho.substring(PASTA_ASSETS.length() + 2)
                : caminho.substring(1);

        Path arquivo = base.resolve(relativo).normalize();
        if (!arquivo.startsWith(base) || !Files.isRegularFile(arquivo)) {
            responder(troca, 404, "text/plain; charset=utf-8",
                    "404 - nao encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] corpo = Files.readAllBytes(arquivo);
        responder(troca, 200, tipoDe(arquivo), corpo);
    }

    private static String tipoDe(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        if (nome.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (nome.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (nome.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (nome.endsWith(".png")) {
            return "image/png";
        }
        if (nome.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (nome.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        return "application/octet-stream";
    }

    static void responder(HttpExchange troca, int status, String tipo, byte[] corpo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }

    static String lerCorpo(HttpExchange troca) throws IOException {
        try (InputStream entrada = troca.getRequestBody()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
