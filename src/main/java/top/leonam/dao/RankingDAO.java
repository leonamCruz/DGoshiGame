package top.leonam.dao;

import top.leonam.bd.BdConfig;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RankingDAO {
    private static final String URL = BdConfig.getUrl();

    public record EntradaRanking(long donoId, String valor) {}

    /** Top 10 por nível (maior XP). */
    public List<EntradaRanking> topPorNivel(int limite) {
        return buscar("""
                SELECT d.id, d.nivel || ' (XP: ' || d.experiencia || ')' AS valor
                FROM dono d
                ORDER BY d.experiencia DESC
                LIMIT ?
                """, limite);
    }

    /** Top 10 por moedas. */
    public List<EntradaRanking> topPorMoedas(int limite) {
        return buscar("""
                SELECT d.id, CAST(d.moedas AS TEXT) AS valor
                FROM dono d
                ORDER BY d.moedas DESC
                LIMIT ?
                """, limite);
    }

    /**
     * Top 10 bichos mais velhos (por data de nascimento, apenas vivos).
     * Retorna donoId e nome do bicho com a idade em dias.
     */
    public List<EntradaRanking> topBichosMaisVelhos(int limite) {
        return buscar("""
                SELECT b.dono_id,
                       b.nome || ' (' || b.tipo || ', ' || b.estagio || ', '
                       || CAST(CAST((julianday('now') - julianday(b.nascimento)) AS INTEGER) AS TEXT)
                       || ' dias)' AS valor
                FROM bicho b
                WHERE b.morto = 0
                ORDER BY b.nascimento ASC
                LIMIT ?
                """, limite);
    }

    private List<EntradaRanking> buscar(String sql, int limite) {
        var lista = new ArrayList<EntradaRanking>();

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limite);
            var rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new EntradaRanking(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            System.err.println("[RankingDAO] " + e.getMessage());
        }

        return lista;
    }
}
