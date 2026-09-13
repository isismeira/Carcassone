## Como rodar

```sh
./run.sh          # compila e sobe em http://localhost:8080
./run.sh 9000     # ou em outra porta
```

Só precisa de um JDK 17 ou mais novo — nenhuma dependência externa.

Para conferir as regras do modelo sem abrir o navegador:

```sh
javac -d build $(find src/java -name '*.java')
java -cp build carcassonne.Principal --testes
```

## Estrutura

```
.
├── run.sh                  compila e sobe o servidor
├── README.md
├── src/
│   ├── java/               código Java
│   │   └── carcassonne/
│   │       ├── modelo/     regras do jogo — não conhece tela, HTTP nem imagens
│   │       ├── controle/   orquestração da partida
│   │       ├── visao/      servidor HTTP, rotas /api e JSON
│   │       ├── Principal   ponto de entrada
│   │       └── Testes      conferência das regras
│   └── web/                interface
│       ├── index.html  index.css  index.js     tela inicial
│       └── jogo.html   jogo.css   jogo.js      tela de partida
├── assets/                 imagens
│   ├── marca/              logo e fundo da tela inicial
│   └── tiles/              as 20 cartelas e o brasão (ver CREDITOS.md)
├── docs/                   requisitos do trabalho
└── build/                  gerado pela compilação (fora do git)
```

Cada pasta tem um único papel: `src` guarda o que é escrito por nós, `assets` o
que é conteúdo, `docs` o que é documento e `build` o que é descartável. Para o
navegador, `src/web` e `assets` aparecem como uma árvore só — a página fica em
`/jogo.html` e as cartelas em `/assets/tiles/`.
