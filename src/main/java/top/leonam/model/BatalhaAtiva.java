package top.leonam.model;

import java.time.OffsetDateTime;

public class BatalhaAtiva {
    private long id;
    private long desafianteId;
    private long desafiadoId;
    private long canalId;
    private long turnoDe;
    private OffsetDateTime criadoEm;
    /** false = desafio pendente de aceite; true = batalha em andamento */
    private boolean aceita;

    public boolean isAceita() {
        return aceita;
    }

    public void setAceita(boolean aceita) {
        this.aceita = aceita;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDesafianteId() {
        return desafianteId;
    }

    public void setDesafianteId(long desafianteId) {
        this.desafianteId = desafianteId;
    }

    public long getDesafiadoId() {
        return desafiadoId;
    }

    public void setDesafiadoId(long desafiadoId) {
        this.desafiadoId = desafiadoId;
    }

    public long getCanalId() {
        return canalId;
    }

    public void setCanalId(long canalId) {
        this.canalId = canalId;
    }

    public long getTurnoDe() {
        return turnoDe;
    }

    public void setTurnoDe(long turnoDe) {
        this.turnoDe = turnoDe;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
