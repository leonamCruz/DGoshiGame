import static top.leonam.bd.Bd.iniciarBd;
import static top.leonam.jda.IniciaJda.iniciaJda;

void main() {
    IO.println("Iniciando o Bot - Aguarde");
    iniciarBd();
    iniciaJda();
    System.gc();
}

