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
 * No Termux (Linux-Android/aarch64) o xerial/sqlite-jdbc não encontra o
 * binário nativo embutido, pois o OS se identifica como "Linux-Android".
 * Solução: apontar para o libsqlite.so do sistema Termux antes de qualquer
 * conexão. O Termux instala sqlite com `pkg install sqlite`.
 *
 * Em qualquer outra plataforma, o driver carrega normalmente.
 */
static void configurarSqliteParaPlataforma() {
    String osName = System.getProperty("os.name", "");
    String prefix = System.getenv("PREFIX"); // definido pelo Termux: /data/data/com.termux/files/usr

    boolean isTermux = osName.contains("Linux-Android")
            || (prefix != null && prefix.contains("com.termux"));

    if (isTermux) {
        String libDir = (prefix != null) ? prefix + "/lib" : "/data/data/com.termux/files/usr/lib";
        System.setProperty("org.sqlite.lib.path", libDir);
        System.setProperty("org.sqlite.lib.name", "libsqlite.so");
        IO.println("Termux detectado — usando SQLite do sistema em: " + libDir);
    }
}
