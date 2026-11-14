package top.leonam.dao;

import top.leonam.bd.BdConfig;
import top.leonam.model.BatalhaAtiva;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class BatalhaDAO {
    private static final String URL = BdConfig.getUrl();

    public void salvar(BatalhaAtiva batalha) {
        var sql = """
                INSERT INTO batalha_ativa (desafiante_id, desafiado_id, canal_id, turno_de, aceita, criado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, batalha.getDesafianteId());
            pstmt.setLong(2, batalha.getDesafiadoId());
            pstmt.setLong(3, batalha.getCanalId());
            pstmt.setLong(4, batalha.getTurnoDe());
            pstmt.setInt(5, batalha.isAceita() ? 1 : 0);
            pstmt.setString(6, batalha.getCriadoEm().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            pstmt.executeUpdate();

            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) batalha.setId(rs.getLong(1));
            }

        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#salvar] " + e.getMessage());
        }
    }

    public BatalhaAtiva buscarPorJogador(long jogadorId) {
        var sql = "SELECT * FROM batalha_ativa WHERE desafiante_id = ? OR desafiado_id = ?";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, jogadorId);
            pstmt.setLong(2, jogadorId);
            var rs = pstmt.executeQuery();

            if (rs.next()) return mapear(rs);

        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#buscarPorJogador] " + e.getMessage());
        }
        return null;
    }

    public void marcarAceita(long batalhaId) {
        var sql = "UPDATE batalha_ativa SET aceita = 1 WHERE id = ?";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, batalhaId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#marcarAceita] " + e.getMessage());
        }
    }

    public void atualizarTurno(long batalhaId, long jogadorId) {
        var sql = "UPDATE batalha_ativa SET turno_de = ? WHERE id = ?";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, jogadorId);
            pstmt.setLong(2, batalhaId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#atualizarTurno] " + e.getMessage());
        }
    }

    public void deletar(long batalhaId) {
        var sql = "DELETE FROM batalha_ativa WHERE id = ?";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, batalhaId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#deletar] " + e.getMessage());
        }
    }

    /**
     * Remove desafios pendentes (não aceitos) com mais de 2 minutos.
     * Chamado no startup do bot para limpar estado órfão de restarts anteriores.
     */
    public void limparPendentesAntigos() {
        // SQLite: datetime('now', '-2 minutes') filtra criados há mais de 2 min
        var sql = "DELETE FROM batalha_ativa WHERE aceita = 0 AND criado_em < datetime('now', '-2 minutes')";

        try (var conn = DriverManager.getConnection(URL);
             var stmt = conn.createStatement()) {
            int removidos = stmt.executeUpdate(sql);
            if (removidos > 0) {
                System.out.println("[BatalhaDAO] Removidos " + removidos + " desafio(s) pendente(s) expirado(s).");
            }
        } catch (SQLException e) {
            System.err.println("[BatalhaDAO#limparPendentesAntigos] " + e.getMessage());
        }
    }

    private BatalhaAtiva mapear(java.sql.ResultSet rs) throws SQLException {
        var batalha = new BatalhaAtiva();
        batalha.setId(rs.getLong("id"));
        batalha.setDesafianteId(rs.getLong("desafiante_id"));
        batalha.setDesafiadoId(rs.getLong("desafiado_id"));
        batalha.setCanalId(rs.getLong("canal_id"));
        batalha.setTurnoDe(rs.getLong("turno_de"));
        batalha.setAceita(rs.getInt("aceita") == 1);
        batalha.setCriadoEm(OffsetDateTime.parse(rs.getString("criado_em"), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return batalha;
    }
}
