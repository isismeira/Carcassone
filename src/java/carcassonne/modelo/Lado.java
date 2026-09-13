package carcassonne.modelo;

/** Os quatro lados de uma cartela. A ordem segue o sentido horario. */
public enum Lado {
    NORTE, LESTE, SUL, OESTE;

    /** @return o lado diametralmente oposto a este. */
    public Lado oposto() {
        return values()[(ordinal() + 2) % 4];
    }

    /** @return o lado obtido girando este em 90 graus para a direita. */
    public Lado proximoHorario() {
        return values()[(ordinal() + 1) % 4];
    }

    /** @return o lado obtido girando este em 90 graus para a esquerda. */
    public Lado proximoAntiHorario() {
        return values()[(ordinal() + 3) % 4];
    }
}
