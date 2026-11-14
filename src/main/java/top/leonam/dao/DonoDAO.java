package top.leonam.dao;

import top.leonam.bd.BdConfig;
import top.leonam.model.Dono;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DonoDAO {
    private static final String URL = BdConfig.getUrl();

    public void salvar(Dono dono) {
        var sql = "INSERT OR REPLACE INTO dono (id, criado_em, ultimo_daily, moedas, experiencia, nivel) VALUES (?, ?, ?, ?, ?, ?)";
        try (var conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, dono.getId());
            pstmt.setString(2, dono.getCriadoEm().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            pstmt.setString(3, dono.getUltimoDaily() != null ? dono.getUltimoDaily().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
            pstmt.setLong(4, dono.getMoedas());
            pstmt.setLong(5, dono.getExperiencia());
            pstmt.setInt(6, dono.getNivel());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DonoDAO#salvar] " + e.getMessage());
        }
    }

    public Dono buscarPorId(long id) {
        var sql = "SELECT * FROM dono WHERE id = ?";
        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            var rs = pstmt.executeQuery();

            if (rs.next()) {
                var dono = new Dono();
                dono.setId(rs.getLong("id"));
                dono.setCriadoEm(OffsetDateTime.parse(rs.getString("criado_em"), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                String ultimoDailyStr = rs.getString("ultimo_daily");

                if (ultimoDailyStr != null) {
                    dono.setUltimoDaily(OffsetDateTime.parse(ultimoDailyStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
                
                dono.setMoedas(rs.getLong("moedas"));
                dono.setExperiencia(rs.getLong("experiencia"));
                return dono;
            }
        } catch (SQLException e) {
            System.err.println("[DonoDAO#buscarPorId] " + e.getMessage());
        }
        return null;
    }
}