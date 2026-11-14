package top.leonam.bd;

import org.sqlite.JDBC;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Bd {
    private static final String CREATE_DATABASE = """
            CREATE TABLE IF NOT EXISTS dono (
                id INTEGER PRIMARY KEY,
                criado_em TEXT NOT NULL,
                ultimo_daily TEXT,
                moedas INTEGER NOT NULL,
                experiencia INTEGER NOT NULL,
                nivel INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS bicho (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dono_id INTEGER NOT NULL,
                nome TEXT NOT NULL,
                nascimento TEXT NOT NULL,
                ultima_interacao TEXT,
                fome INTEGER NOT NULL,
                vida INTEGER NOT NULL,
                limpeza INTEGER NOT NULL,
                felicidade INTEGER NOT NULL DEFAULT 100,
                morto INTEGER NOT NULL DEFAULT 0,
                estagio TEXT NOT NULL,
                tipo TEXT NOT NULL,
                bonus_ataque_treino INTEGER NOT NULL DEFAULT 0,
                ultimo_treino TEXT,
                FOREIGN KEY (dono_id) REFERENCES dono(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS inventario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dono_id INTEGER NOT NULL,
                tipo_item TEXT NOT NULL,
                quantidade INTEGER NOT NULL,
                FOREIGN KEY (dono_id) REFERENCES dono(id) ON DELETE CASCADE,
                UNIQUE(dono_id, tipo_item)
            );

            CREATE TABLE IF NOT EXISTS batalha_ativa (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                desafiante_id INTEGER NOT NULL,
                desafiado_id INTEGER NOT NULL,
                canal_id INTEGER NOT NULL,
                turno_de INTEGER NOT NULL,
                aceita INTEGER NOT NULL DEFAULT 0,
                criado_em TEXT NOT NULL,
                FOREIGN KEY (desafiante_id) REFERENCES dono(id),
                FOREIGN KEY (desafiado_id) REFERENCES dono(id)
            );
            """;

    public static void iniciarBd() {
        var url = BdConfig.getUrl();

        // Para o banco de arquivo, usa o validador do JDBC; para :memory: segue direto
        if (url.startsWith("jdbc:sqlite:") && !url.contains(":memory:")) {
            var path = url.replace("jdbc:sqlite:", "");
            if (!org.sqlite.JDBC.isValidURL(url)) IO.println("Url Inválido: " + path);
        }

        try (var conn = DriverManager.getConnection(url)) {

            try (var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA SYNCHRONOUS = NORMAL;");
                stmt.execute("PRAGMA JOURNAL_MODE = WAL;");
            }

            conn.setAutoCommit(false);
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate(CREATE_DATABASE);
            }
            conn.commit();

            // Migração: adiciona colunas novas em bancos existentes (ignora se já existem)
            migrarBanco(conn);
            conn.commit();

        } catch (SQLException e) {
            IO.println(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private static void migrarBanco(java.sql.Connection conn) throws SQLException {
        String[] migracoes = {
            "ALTER TABLE bicho ADD COLUMN bonus_ataque_treino INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE bicho ADD COLUMN ultimo_treino TEXT",
        };
        try (var stmt = conn.createStatement()) {
            for (String sql : migracoes) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Coluna já existe — ignora silenciosamente
                    if (!e.getMessage().contains("duplicate column")) {
                        throw e;
                    }
                }
            }
        }
    }
}