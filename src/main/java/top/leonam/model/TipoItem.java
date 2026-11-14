package top.leonam.model;

public enum TipoItem {
    RACAO_COMUM("Ração Comum", 15, 50, Efeito.FOME),
    RACAO_PREMIUM("Ração Premium", 40, 150, Efeito.FOME),
    RACAO_GOURMET("Ração Gourmet", 30, 100, Efeito.FOME_E_FELICIDADE),
    COMIDA_ESPECIAL("Comida Especial", 60, 300, Efeito.FOME_E_VIDA),
    MEDICAMENTO("Medicamento", 50, 200, Efeito.VIDA),
    VACINA("Vacina", 30, 180, Efeito.LIMPEZA),
    BRINQUEDO("Brinquedo", 20, 80, Efeito.FELICIDADE),
    SUPER_BRINQUEDO("Super Brinquedo", 50, 150, Efeito.FELICIDADE),
    POCAO_CURA_TOTAL("Poção de Cura Total", 0, 500, Efeito.CURA_TOTAL);

    public enum Efeito {
        FOME, FOME_E_VIDA, FOME_E_FELICIDADE, VIDA, LIMPEZA, FELICIDADE, CURA_TOTAL
    }

    private final String nome;
    private final int poder;
    private final int preco;
    private final Efeito efeito;

    TipoItem(String nome, int poder, int preco, Efeito efeito) {
        this.nome = nome;
        this.poder = poder;
        this.preco = preco;
        this.efeito = efeito;
    }

    public String getNome() {
        return nome;
    }

    public int getPoder() {
        return poder;
    }

    public int getPreco() {
        return preco;
    }

    public Efeito getEfeito() {
        return efeito;
    }
}