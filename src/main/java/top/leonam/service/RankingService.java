package top.leonam.service;

import top.leonam.dao.RankingDAO;
import top.leonam.dao.RankingDAO.EntradaRanking;

import java.util.List;

public class RankingService {
    private static final int LIMITE = 10;

    private final RankingDAO rankingDAO = new RankingDAO();

    /**
     * Retorna o ranking formatado para exibição no Discord.
     *
     * @param categoria "nivel", "moedas" ou "bichos"
     */
    public RankingResultado buscarRanking(String categoria) {
        return switch (categoria.toLowerCase()) {
            case "nivel"  -> formatar("🏅 Ranking — Maior Nível",
                                     rankingDAO.topPorNivel(LIMITE),
                                     id -> "Nível ", true);
            case "moedas" -> formatar("💰 Ranking — Mais Moedas",
                                     rankingDAO.topPorMoedas(LIMITE),
                                     id -> "", false);
            case "bichos" -> formatar("🐾 Ranking — Bichos Mais Velhos",
                                     rankingDAO.topBichosMaisVelhos(LIMITE),
                                     id -> "", false);
            default       -> new RankingResultado("Categoria inválida. Use: `nivel`, `moedas` ou `bichos`.", true);
        };
    }

    private RankingResultado formatar(String titulo, List<EntradaRanking> entradas,
                                      java.util.function.LongFunction<String> prefixo,
                                      boolean mostrarPrefixo) {
        if (entradas.isEmpty()) {
            return new RankingResultado("Nenhum dado encontrado ainda.", false);
        }

        var medalhas = new String[]{"🥇", "🥈", "🥉"};
        var sb = new StringBuilder("## " + titulo + "\n\n");

        for (int i = 0; i < entradas.size(); i++) {
            var entrada = entradas.get(i);
            String posicao = i < medalhas.length ? medalhas[i] : "**" + (i + 1) + ".**";
            sb.append(posicao)
              .append(" <@").append(entrada.donoId()).append("> — ")
              .append(mostrarPrefixo ? prefixo.apply(entrada.donoId()) : "")
              .append(entrada.valor())
              .append("\n");
        }

        return new RankingResultado(sb.toString(), false);
    }

    public record RankingResultado(String texto, boolean erro) {}
}
