package top.leonam.bd;

/**
 * Centraliza o URL do banco de dados.
 * Em testes, defina a variável de ambiente DB_URL=jdbc:sqlite::memory:
 * para usar banco em memória isolado.
 */
public class BdConfig {
    private static final String DEFAULT_URL = "jdbc:sqlite:dados.db";

    public static String getUrl() {
        var env = System.getenv("DB_URL");
        return (env != null && !env.isBlank()) ? env : DEFAULT_URL;
    }
}
