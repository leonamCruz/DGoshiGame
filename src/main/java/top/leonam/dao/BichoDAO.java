package top.leonam.dao;

import top.leonam.bd.BdConfig;
import top.leonam.model.Bicho;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BichoDAO {
    private static final String URL = BdConfig.getUrl();

    public void salvar(Bicho bicho) {
        var sql = """
                INSERT OR REPLACE INTO bicho
                    (id, dono_id, nome, nascimento, ultima_interacao, fome, vida, limpeza, felicidade, morto, estagio, tipo, bonus_ataque_treino, ultimo_treino)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {

            if (bicho.getId() == 0) pstmt.setNull(1, Types.INTEGER);
            else pstmt.setLong(1, bicho.getId());

            pstmt.setLong(2, bicho.getDonoId());
            pstmt.setString(3, bicho.getNome());
            pstmt.setString(4, bicho.getNascimento().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            pstmt.setString(5, bicho.getUltimaInteracao() != null
                    ? bicho.getUltimaInteracao().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : null);
            pstmt.setInt(6, bicho.getFome());
            pstmt.setInt(7, bicho.getVida());
            pstmt.setInt(8, bicho.getLimpeza());
            pstmt.setInt(9, bicho.getFelicidade());
            pstmt.setInt(10, bicho.isMorto() ? 1 : 0);
            pstmt.setString(11, bicho.getEstagio());
            pstmt.setString(12, bicho.getTipo());
            pstmt.setInt(13, bicho.getBonusAtaqueTreino());
            pstmt.setString(14, bicho.getUltimoTreino() != null
                    ? bicho.getUltimoTreino().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : null);

            pstmt.executeUpdate();

            if (bicho.getId() == 0) {
                try (var rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) bicho.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("[BichoDAO#salvar] " + e.getMessage());
        }
    }

    public Bicho buscarPorDonoId(long donoId) {
        var sql = "SELECT * FROM bicho WHERE dono_id = ? AND morto = 0";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, donoId);
            var rs = pstmt.executeQuery();

            if (rs.next()) return mapear(rs);

        } catch (SQLException e) {
            System.err.println("[BichoDAO#buscarPorDonoId] " + e.getMessage());
        }
        return null;
    }

    public List<Bicho> buscarTodos() {
        var bichos = new ArrayList<Bicho>();
        var sql = "SELECT * FROM bicho WHERE morto = 0";

        try (var conn = DriverManager.getConnection(URL);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) bichos.add(mapear(rs));

        } catch (SQLException e) {
            System.err.println("[BichoDAO#buscarTodos] " + e.getMessage());
        }
        return bichos;
    }

    public void marcarMorto(long bichoId) {
        var sql = "UPDATE bicho SET morto = 1 WHERE id = ?";

        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, bichoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BichoDAO#marcarMorto] " + e.getMessage());
        }
    }

    private Bicho mapear(ResultSet rs) throws SQLException {
        var bicho = new Bicho();
        bicho.setId(rs.getLong("id"));
        bicho.setDonoId(rs.getLong("dono_id"));
        bicho.setNome(rs.getString("nome"));
        bicho.setNascimento(OffsetDateTime.parse(rs.getString("nascimento"), DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        var ultimaInteracao = rs.getString("ultima_interacao");
        if (ultimaInteracao != null) {
            bicho.setUltimaInteracao(OffsetDateTime.parse(ultimaInteracao, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        bicho.setFome(rs.getInt("fome"));
        bicho.setVida(rs.getInt("vida"));
        bicho.setLimpeza(rs.getInt("limpeza"));
        bicho.setFelicidade(rs.getInt("felicidade"));
        bicho.setMorto(rs.getInt("morto") == 1);
        bicho.setEstagio(rs.getString("estagio"));
        bicho.setTipo(rs.getString("tipo"));
        bicho.setBonusAtaqueTreino(rs.getInt("bonus_ataque_treino"));
        var ultimoTreino = rs.getString("ultimo_treino");
        if (ultimoTreino != null) {
            bicho.setUltimoTreino(OffsetDateTime.parse(ultimoTreino, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return bicho;
    }
}
