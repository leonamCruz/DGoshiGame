package top.leonam.service;

import top.leonam.dao.DonoDAO;
import top.leonam.dao.InventarioDAO;
import top.leonam.model.Dono;
import top.leonam.model.ItemInventario;
import top.leonam.model.TipoItem;

public class LojaService {
    private final DonoDAO donoDAO = new DonoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();

    public String listarLoja() {
        var sb = new StringBuilder("🏪 **Loja do Goshi**\n\n");

        for (TipoItem item : TipoItem.values()) {
            sb.append(String.format("• **%s** — %d moedas  (%s +%d)\n",
                    item.getNome(),
                    item.getPreco(),
                    descricaoEfeito(item),
                    item.getPoder()));
        }

        sb.append("\nUse `/comprar <item>` para adquirir.");
        return sb.toString();
    }

    public String comprar(long donoId, String nomeItem) {
        TipoItem tipoItem = resolverItem(nomeItem);
        if (tipoItem == null) {
            return "Item **" + nomeItem + "** não encontrado. Use `/loja` para ver os itens disponíveis.";
        }

        Dono dono = donoDAO.buscarPorId(donoId);
        if (dono == null) {
            return "Você ainda não tem um bicho! Use `/adotar` primeiro.";
        }

        if (dono.getMoedas() < tipoItem.getPreco()) {
            return "Moedas insuficientes! Você tem **" + dono.getMoedas() + "** moedas e precisa de **"
                    + tipoItem.getPreco() + "**.";
        }

        dono.removerMoedas(tipoItem.getPreco());
        donoDAO.salvar(dono);

        var itens = inventarioDAO.listarPorDono(donoId);
        var itemExistente = itens.stream()
                .filter(i -> i.getTipoItem() == tipoItem)
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            itemExistente.adicionar(1);
            inventarioDAO.salvarOuAtualizar(itemExistente);
        } else {
            inventarioDAO.salvarOuAtualizar(new ItemInventario(donoId, tipoItem, 1));
        }

        return "Comprado! **" + tipoItem.getNome() + "** adicionado ao inventário. Saldo restante: "
                + dono.getMoedas() + " moedas.";
    }

    public String listarInventario(long donoId) {
        var itens = inventarioDAO.listarPorDono(donoId);
        if (itens.isEmpty()) return "Seu inventário está vazio.";

        var sb = new StringBuilder("🎒 **Inventário**\n\n");
        for (var item : itens) {
            if (item.getQuantidade() > 0) {
                sb.append(String.format("• **%s** — %d unidade(s)\n",
                        item.getTipoItem().getNome(),
                        item.getQuantidade()));
            }
        }
        return sb.toString();
    }

    private TipoItem resolverItem(String nomeItem) {
        if (nomeItem == null) return null;
        String normalizado = nomeItem.trim().toUpperCase().replace(" ", "_").replace("Ã", "A").replace("Ç", "C");

        for (TipoItem item : TipoItem.values()) {
            if (item.name().equals(normalizado)) return item;
            if (item.getNome().equalsIgnoreCase(nomeItem.trim())) return item;
        }
        return null;
    }

    private String descricaoEfeito(TipoItem item) {
        return switch (item.getEfeito()) {
            case FOME              -> "Fome";
            case FOME_E_VIDA       -> "Fome + Vida";
            case FOME_E_FELICIDADE -> "Fome + Felicidade";
            case VIDA              -> "Vida";
            case LIMPEZA           -> "Limpeza";
            case FELICIDADE        -> "Felicidade";
            case CURA_TOTAL        -> "Cura Total (Vida, Fome, Limpeza, Felicidade)";
        };
    }
}
