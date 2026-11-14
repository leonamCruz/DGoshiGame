package top.leonam.dao;

import top.leonam.bd.BdConfig;
import top.leonam.model.ItemInventario;
import top.leonam.model.TipoItem;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {
    private static final String URL = BdConfig.getUrl();

    public void salvarOuAtualizar(ItemInventario item) {
        var sql = "INSERT INTO inventario (dono_id, tipo_item, quantidade) VALUES (?, ?, ?) ON CONFLICT(dono_id, tipo_item) DO UPDATE SET quantidade = excluded.quantidade";
        
        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, item.getDonoId());
            pstmt.setString(2, item.getTipoItem().name());
            pstmt.setInt(3, item.getQuantidade());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[InventarioDAO#salvarOuAtualizar] " + e.getMessage());
        }
    }

    public List<ItemInventario> listarPorDono(long donoId) {
        var itens = new ArrayList<ItemInventario>();
        var sql = "SELECT * FROM inventario WHERE dono_id = ?";
        try (var conn = DriverManager.getConnection(URL);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, donoId);
            var rs = pstmt.executeQuery();

            while (rs.next()) {
                ItemInventario item = new ItemInventario();
                item.setId(rs.getLong("id"));
                item.setDonoId(rs.getLong("dono_id"));
                item.setTipoItem(TipoItem.valueOf(rs.getString("tipo_item")));
                item.setQuantidade(rs.getInt("quantidade"));
                itens.add(item);
            }

        } catch (SQLException e) {
            System.err.println("[InventarioDAO#listarPorDono] " + e.getMessage());
        }

        return itens;
    }
}