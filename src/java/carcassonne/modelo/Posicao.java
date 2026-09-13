package carcassonne.modelo;

import java.util.Objects;

/**
 * Coordenada bidimensional do tabuleiro (RF03.2). Imutavel, para servir com
 * seguranca de chave em um HashMap (RNF02.1).
 */
public final class Posicao {
    private final int x;
    private final int y;

    public Posicao(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * @param lado direcao desejada.
     * @return a posicao imediatamente adjacente naquele lado. O eixo Y cresce para o sul.
     */
    public Posicao vizinha(Lado lado) {
        return switch (lado) {
            case NORTE -> new Posicao(x, y - 1);
            case LESTE -> new Posicao(x + 1, y);
            case SUL -> new Posicao(x, y + 1);
            case OESTE -> new Posicao(x - 1, y);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Posicao outra && outra.x == x && outra.y == y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
