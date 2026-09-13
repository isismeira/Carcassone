# Créditos das imagens das peças

As imagens desta pasta vêm do projeto open-source **Carcassonne**, de
**Timur Saglam** (`tsaglam`), distribuído sob a **Eclipse Public License 2.0**.

- Repositório: <https://github.com/tsaglam/Carcassonne>
- Pasta de origem: `src/main/resources/tiles` (e `emblem.png` na raiz de `resources`)

As imagens foram reduzidas de 500×500 para 256×256 pixels para aliviar o
carregamento da página; nada além disso foi alterado.

Só a rotação `0` de cada tipo foi baixada: as demais rotações são obtidas na
tela com `transform: rotate(90deg × n)`, que é geometricamente equivalente.

A tabela de terrenos das cartelas em `Baralho.java` também foi derivada do
`TileType.java` do mesmo projeto.
