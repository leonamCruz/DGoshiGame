import org.sqlite.core.NativeDB;

import java.lang.reflect.Field;

import static top.leonam.bd.Bd.iniciarBd;
import static top.leonam.jda.IniciaJda.iniciaJda;

void main() {
    IO.println("Iniciando o Bot - Aguarde");
    configurarSqliteParaPlataforma();
    iniciarBd();
    iniciaJda();
    System.gc();
}

/**
 * No Termux (Linux-Android/aarch64) o xerial/sqlite-jdbc não inclui o
 * binário nativo para essa plataforma.
 *
 * Solução: carregar o libsqlite.so do sistema manualmente com System.load()
 * e marcar NativeDB como já inicializada via reflexão, para que o
 * SQLiteJDBCLoader não tente (e falhe em) extrair o binário embutido.
 *
 * Requer `pkg install sqlite` no Termux.
 */
static void configurarSqliteParaPlataforma() {
    String osName = System.getProperty("os.name", "");
    String prefix = System.getenv("PREFIX"); // /data/data/com.termux/files/usr

    boolean isTermux = osName.contains("Linux-Android")
            || (prefix != null && prefix.contains("com.termux"));

    if (!isTermux) return;

    String libDir = (prefix != null) ? prefix + "/lib" : "/data/data/com.termux/files/usr/lib";
    IO.println("Termux detectado — carregando SQLite do sistema: " + libDir);

    // Tenta libsqlitejdbc.so primeiro (se existir), senão usa libsqlite.so do sistema
    boolean carregou = tentarCarregarLib(libDir + "/libsqlitejdbc.so")
                    || tentarCarregarLib(libDir + "/libsqlite.so");

    if (!carregou) {
        IO.println("AVISO: não encontrou libsqlite.so em " + libDir
                + ". Instale com: pkg install sqlite");
        return;
    }

    // Marca NativeDB como inicializada para que o SQLiteJDBCLoader não tente novamente
    try {
        Field isLoaded = NativeDB.class.getDeclaredField("isLoaded");
        isLoaded.setAccessible(true);
        isLoaded.set(null, true);

        Field loadSucceeded = NativeDB.class.getDeclaredField("loadSucceeded");
        loadSucceeded.setAccessible(true);
        loadSucceeded.set(null, true);

        IO.println("SQLite do sistema carregado com sucesso.");
    } catch (Exception e) {
        IO.println("Aviso reflexão NativeDB: " + e.getMessage());
    }
}

static boolean tentarCarregarLib(String path) {
    try {
        System.load(path);
        return true;
    } catch (UnsatisfiedLinkError | SecurityException e) {
        return false;
    }
}
