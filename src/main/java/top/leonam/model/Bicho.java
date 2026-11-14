package top.leonam.model;

import java.time.Duration;
import java.time.OffsetDateTime;

public class Bicho {
    public static final int MAX_STATUS = 100;
    public static final int MIN_STATUS = 0;
    public static final int FOME_POR_HORA        = 5;
    public static final int LIMPEZA_POR_HORA      = 2;
    public static final int FELICIDADE_POR_HORA   = 1;
    public static final int VIDA_PERDIDA_SEM_FOME_POR_HORA    = 3;
    public static final int VIDA_PERDIDA_SEM_LIMPEZA_POR_HORA = 2;

    private long id;
    private long donoId;
    private String nome;
    private OffsetDateTime nascimento;
    private OffsetDateTime ultimaInteracao;
    private int fome;
    private int vida;
    private int limpeza;
    private int felicidade;
    private String estagio;
    private String tipo;
    private boolean morto;
    private int bonusAtaqueTreino;
    private OffsetDateTime ultimoTreino;

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Calcula os estágios de vida com base na idade em dias.
     * Ovo (0-1 dia) → Filhote (1-7 dias) → Jovem (7-30 dias) → Adulto (30+ dias)
     */
    public String calcularEstagio() {
        if (nascimento == null) return "Filhote";
        long dias = Duration.between(nascimento, OffsetDateTime.now()).toDays();
        if (dias < 1)   return "Ovo";
        if (dias < 7)   return "Filhote";
        if (dias < 30)  return "Jovem";
        return "Adulto";
    }

    /**
     * Atualiza todos os status com base no tempo real desde a última interação.
     * Fome cai 5/hora. Limpeza cai 2/hora.
     * Com fome=0, vida cai 3/hora. Com limpeza=0, vida cai 2/hora.
     * Atualiza também o estágio. Marca o bicho como morto se vida <= 0.
     */
    public void atualizarStatusPeloTempo() {
        if (this.ultimaInteracao == null) return;

        long horas = Duration.between(this.ultimaInteracao, OffsetDateTime.now()).toHours();
        if (horas <= 0) return;

        // Decaimento de fome
        int novaFome = Math.max(MIN_STATUS, this.fome - (int)(horas * FOME_POR_HORA));

        // Decaimento de limpeza
        int novaLimpeza = Math.max(MIN_STATUS, this.limpeza - (int)(horas * LIMPEZA_POR_HORA));

        // Decaimento de felicidade (1/hora, mínimo 10 — bicho nunca fica completamente infeliz por tempo)
        int novaFelicidade = Math.max(10, this.felicidade - (int)(horas * FELICIDADE_POR_HORA));

        // Dano à vida por fome zero e/ou sujeira
        int horasComFomeZerada    = novaFome    == 0 ? (int) horas : 0;
        int horasComLimpezaZerada = novaLimpeza == 0 ? (int) horas : 0;
        int danoVida = (horasComFomeZerada * VIDA_PERDIDA_SEM_FOME_POR_HORA)
                     + (horasComLimpezaZerada * VIDA_PERDIDA_SEM_LIMPEZA_POR_HORA);
        int novaVida = Math.max(MIN_STATUS, this.vida - danoVida);

        this.fome       = novaFome;
        this.limpeza    = novaLimpeza;
        this.felicidade = novaFelicidade;
        this.vida       = novaVida;
        this.estagio    = calcularEstagio();
        this.ultimaInteracao = OffsetDateTime.now();

        if (this.vida <= MIN_STATUS) {
            this.morto = true;
        }
    }

    /** Retorna o nível de ataque base do bicho (usado em batalha).
     *  Bichos mais velhos e mais felizes causam mais dano.
     *  Treinos aumentam o ataque permanentemente. */
    public int calcularAtaqueBase() {
        int bonus = switch (calcularEstagio()) {
            case "Jovem"  -> 5;
            case "Adulto" -> 15;
            default       -> 0;
        };
        // Bônus de felicidade: até +5 quando felicidade >= 70
        int bonusFelicidade = felicidade >= 70 ? 5 : 0;
        // Bônus de treino: permanente, acumula com cada treino
        // Ataque base: entre 10 e 24, mais bônus de estágio, felicidade e treino
        return 10 + bonus + bonusFelicidade + bonusAtaqueTreino + (int)(Math.random() * 15);
    }
    public OffsetDateTime getUltimaInteracao() {
        return ultimaInteracao;
    }

    public void setUltimaInteracao(OffsetDateTime ultimaInteracao) {
        if (ultimaInteracao == null) throw new IllegalArgumentException("A última interação não pode ser nula.");
        this.ultimaInteracao = ultimaInteracao;
    }

    public long getId() {
        return id;
    }

    public long getDonoId() {
        return donoId;
    }

    public void setDonoId(long donoId) {
        if (donoId <= 0) throw new IllegalArgumentException("O ID do dono deve ser válido.");
        this.donoId = donoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().length() < 2) {
            throw new IllegalArgumentException("O nome deve ter no mínimo 2 caracteres.");
        }
        this.nome = nome;
    }

    public OffsetDateTime getNascimento() {
        return nascimento;
    }

    public void setNascimento(OffsetDateTime nascimento) {
        if (nascimento == null) throw new IllegalArgumentException("A data de nascimento não pode ser nula.");
        this.nascimento = nascimento;
    }

    public int getFome() {
        return fome;
    }

    public void setFome(int fome) {
        if (fome < MIN_STATUS || fome > MAX_STATUS) {
            throw new IllegalArgumentException("A fome deve estar entre " + MIN_STATUS + " e " + MAX_STATUS);
        }
        this.fome = fome;
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        if (vida < MIN_STATUS || vida > MAX_STATUS)
            throw new IllegalArgumentException("A vida deve estar entre " + MIN_STATUS + " e " + MAX_STATUS);
        this.vida = vida;
    }

    public int getLimpeza() {
        return limpeza;
    }

    public void setLimpeza(int limpeza) {
        if (limpeza < MIN_STATUS || limpeza > MAX_STATUS)
            throw new IllegalArgumentException("A limpeza deve estar entre " + MIN_STATUS + " e " + MAX_STATUS);
        this.limpeza = limpeza;
    }

    public String getEstagio() {
        return estagio;
    }

    public void setEstagio(String estagio) {
        if (estagio == null || estagio.isBlank()) throw new IllegalArgumentException("O estágio não pode ser vazio.");
        this.estagio = estagio;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("O tipo não pode ser vazio.");
        this.tipo = tipo;
    }

    public int getFelicidade() {
        return felicidade;
    }

    public void setFelicidade(int felicidade) {
        if (felicidade < MIN_STATUS || felicidade > MAX_STATUS)
            throw new IllegalArgumentException("A felicidade deve estar entre " + MIN_STATUS + " e " + MAX_STATUS);
        this.felicidade = felicidade;
    }

    public boolean isMorto() {
        return morto;
    }

    public void setMorto(boolean morto) {
        this.morto = morto;
    }

    public int getBonusAtaqueTreino() {
        return bonusAtaqueTreino;
    }

    public void setBonusAtaqueTreino(int bonusAtaqueTreino) {
        if (bonusAtaqueTreino < 0) throw new IllegalArgumentException("Bônus de treino não pode ser negativo.");
        this.bonusAtaqueTreino = bonusAtaqueTreino;
    }

    public OffsetDateTime getUltimoTreino() {
        return ultimoTreino;
    }

    public void setUltimoTreino(OffsetDateTime ultimoTreino) {
        this.ultimoTreino = ultimoTreino;
    }
}