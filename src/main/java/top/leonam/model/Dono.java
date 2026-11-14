package top.leonam.model;

import java.time.OffsetDateTime;

public class Dono {
    public static final int MOEDAS_INICIAIS = 100;

    private long id;
    private OffsetDateTime criadoEm;
    private OffsetDateTime ultimoDaily;
    private long moedas;
    private long experiencia;
    private int nivel;
    private int nivelAnterior;

    public Dono() {
    }

    public Dono(long id) {
        setId(id);
        this.criadoEm = OffsetDateTime.now();
        this.moedas = MOEDAS_INICIAIS;
        this.experiencia = 0;
        this.nivel = 1;
    }

    public void adicionarMoedas(long quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade de moedas adicionadas deve ser positiva.");
        this.moedas += quantidade;
    }

    public void removerMoedas(long quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade de moedas removidas deve ser positiva.");
        if (this.moedas < quantidade) throw new IllegalStateException("Saldo insuficiente de moedas.");
        this.moedas -= quantidade;
    }

    public void adicionarExperiencia(long quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A experiência adicionada deve ser positiva.");
        this.nivelAnterior = this.nivel;
        this.experiencia += quantidade;
        atualizarNivel();
        // Recompensa automática de level-up: 200 * novo nível em moedas
        if (this.nivel > this.nivelAnterior) {
            this.moedas += 200L * this.nivel;
        }
    }

    /** Retorna true se o último adicionarExperiencia causou level-up. */
    public boolean subiuNivel() {
        return this.nivel > this.nivelAnterior;
    }

    private void atualizarNivel() {
        this.nivel = (int) (this.experiencia / 1000) + 1;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        if (id <= 0) throw new IllegalArgumentException("O ID deve ser maior que zero.");
        this.id = id;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        if (criadoEm == null) throw new IllegalArgumentException("A data de criação não pode ser nula.");
        this.criadoEm = criadoEm;
    }

    public OffsetDateTime getUltimoDaily() {
        return ultimoDaily;
    }

    public void setUltimoDaily(OffsetDateTime ultimoDaily) {
        this.ultimoDaily = ultimoDaily;
    }

    public long getMoedas() {
        return moedas;
    }

    public void setMoedas(long moedas) {
        if (moedas < 0) throw new IllegalArgumentException("O saldo de moedas não pode ser negativo.");
        this.moedas = moedas;
    }

    public long getExperiencia() {
        return experiencia;
    }

    public void setExperiencia(long experiencia) {
        if (experiencia < 0) throw new IllegalArgumentException("A experiência não pode ser negativa.");
        this.experiencia = experiencia;
        atualizarNivel();
        this.nivelAnterior = this.nivel; // Evita falso positivo de subiuNivel() ao carregar do BD
    }

    public int getNivel() {
        return nivel;
    }
}