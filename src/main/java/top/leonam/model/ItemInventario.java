package top.leonam.model;

public class ItemInventario {
    private long id;
    private long donoId;
    private TipoItem tipoItem;
    private int quantidade;

    public ItemInventario() {}

    public ItemInventario(long donoId, TipoItem tipoItem, int quantidade) {
        setDonoId(donoId);
        setTipoItem(tipoItem);
        setQuantidade(quantidade);
    }

    public void adicionar(int qtd) {
        if (qtd <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva.");
        this.quantidade += qtd;
    }

    public void remover(int qtd) {
        if (qtd <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva.");
        if (this.quantidade < qtd) throw new IllegalStateException("Quantidade insuficiente no inventário.");
        this.quantidade -= qtd;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        if (id <= 0) throw new IllegalArgumentException("O ID deve ser maior que zero.");
        this.id = id;
    }

    public long getDonoId() {
        return donoId;
    }

    public void setDonoId(long donoId) {
        if (donoId <= 0) throw new IllegalArgumentException("O ID do dono deve ser válido.");
        this.donoId = donoId;
    }

    public TipoItem getTipoItem() {
        return tipoItem;
    }

    public void setTipoItem(TipoItem tipoItem) {
        if (tipoItem == null) throw new IllegalArgumentException("O tipo do item não pode ser nulo.");
        this.tipoItem = tipoItem;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        if (quantidade < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa.");
        this.quantidade = quantidade;
    }
}