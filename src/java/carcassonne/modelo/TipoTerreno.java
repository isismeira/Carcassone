package carcassonne.modelo;

/** Tipos de terreno que um segmento pode representar. */
public enum TipoTerreno {
    CIDADE, ESTRADA, CAMPO, MOSTEIRO;

    /** @return o papel que um meeple assume ao ocupar um segmento deste terreno (RF04.2). */
    public TipoMeeple papelDoMeeple() {
        return switch (this) {
            case ESTRADA -> TipoMeeple.LADRAO;
            case CIDADE -> TipoMeeple.CAVALEIRO;
            case MOSTEIRO -> TipoMeeple.MONGE;
            case CAMPO -> TipoMeeple.FAZENDEIRO;
        };
    }
}
