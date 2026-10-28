package carcassonne.modelo;

/** Seguidor de um jogador. O papel vem do terreno que ele ocupa (RF04.2). */
public class Meeple {
    private boolean deitado;
    private final Jogador dono;
    private Segmento segmento;

    public Meeple(Jogador dono) {
        this.dono = dono;
    }

    public Jogador getDono() {
        return dono;
    }

    public Segmento getSegmento() {
        return segmento;
    }

    public boolean estaDeitado() {
        return deitado;
    }

    /** O fazendeiro e representado deitado no tabuleiro. */
    public void setDeitado(boolean deitado) {
        this.deitado = deitado;
    }

    /** @return o papel conforme o terreno ocupado, ou null se estiver na reserva. */
    public TipoMeeple getTipo() {
        return segmento == null ? null : segmento.getTipo().papelDoMeeple();
    }

    /** Coloca o meeple sobre um segmento apenas se a regiao ainda estiver livre. */
    public boolean ocupar(Segmento segmento) {
        if (this.segmento != null || segmento == null || segmento.getRegiao() == null
                || !segmento.getRegiao().adicionarMeeple(this)) {
            return false;
        }
        this.segmento = segmento;
        this.deitado = segmento.getTipo() == TipoTerreno.CAMPO;
        return true;
    }

    /** Devolve o meeple a reserva do dono (RF04.4). */
    public void liberar() {
        this.segmento = null;
        this.deitado = false;
        if (dono != null) {
            dono.devolverMeeple(this);
        }
    }
}
