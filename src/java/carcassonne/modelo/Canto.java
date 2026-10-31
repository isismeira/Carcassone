package carcassonne.modelo;

import java.util.List;

/** Quina de uma cartela, usada para conectar campos que se encontram no tabuleiro. */
public enum Canto {
    NOROESTE,
    NORDESTE,
    SUDESTE,
    SUDOESTE;

    /** @return os dois lados da cartela que se encontram nesta quina. */
    public List<Lado> lados() {
        return switch (this) {
            case NOROESTE -> List.of(Lado.NORTE, Lado.OESTE);
            case NORDESTE -> List.of(Lado.NORTE, Lado.LESTE);
            case SUDESTE -> List.of(Lado.SUL, Lado.LESTE);
            case SUDOESTE -> List.of(Lado.SUL, Lado.OESTE);
        };
    }

    /**
     * @param lado um dos dois lados desta quina.
     * @return a quina da cartela vizinha naquele lado que encosta nesta,
     *         espelhada pela borda compartilhada.
     */
    public Canto espelhadoEm(Lado lado) {
        boolean bordaHorizontal = lado == Lado.NORTE || lado == Lado.SUL;
        return switch (this) {
            case NOROESTE -> bordaHorizontal ? SUDOESTE : NORDESTE;
            case NORDESTE -> bordaHorizontal ? SUDESTE : NOROESTE;
            case SUDESTE -> bordaHorizontal ? NORDESTE : SUDOESTE;
            case SUDOESTE -> bordaHorizontal ? NOROESTE : SUDESTE;
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
}
