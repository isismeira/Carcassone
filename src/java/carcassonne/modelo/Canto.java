package carcassonne.modelo;

/** Quina de uma cartela, usada para conectar campos que se encontram no tabuleiro. */
public enum Canto {
    NOROESTE,
    NORDESTE,
    SUDESTE,
    SUDOESTE;

    public Canto oposto() {
        return switch (this) {
            case NOROESTE -> SUDESTE;
            case NORDESTE -> SUDOESTE;
            case SUDESTE -> NOROESTE;
            case SUDOESTE -> NORDESTE;
        };
    }

    public Canto proximoHorario() {
        return switch (this) {
            case NOROESTE -> NORDESTE;
            case NORDESTE -> SUDESTE;
            case SUDESTE -> SUDOESTE;
            case SUDOESTE -> NOROESTE;
        };
    }

    public Canto proximoAntiHorario() {
        return switch (this) {
            case NOROESTE -> SUDOESTE;
            case NORDESTE -> NOROESTE;
            case SUDESTE -> NORDESTE;
            case SUDOESTE -> SUDESTE;
        };
    }

    public Posicao vizinha(Posicao posicao) {
        return switch (this) {
            case NOROESTE -> new Posicao(posicao.getX() - 1, posicao.getY() - 1);
            case NORDESTE -> new Posicao(posicao.getX() + 1, posicao.getY() - 1);
            case SUDESTE -> new Posicao(posicao.getX() + 1, posicao.getY() + 1);
            case SUDOESTE -> new Posicao(posicao.getX() - 1, posicao.getY() + 1);
        };
    }
}
