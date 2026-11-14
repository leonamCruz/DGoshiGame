package top.leonam.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.leonam.bd.Bd;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class BatalhaServiceTest {
    private final BatalhaService    batalha    = new BatalhaService();
    private final TamagotchiService tamagotchi = new TamagotchiService();

    static final String URL = "jdbc:sqlite:dados.db";

    // IDs separados para não conflitar com TamagotchiServiceTest
    private static final long JOGADOR_A  = 800_000_001L;
    private static final long JOGADOR_B  = 800_000_002L;
    private static final long JOGADOR_C  = 800_000_003L;
    private static final long JOGADOR_D  = 800_000_004L;
    private static final long JOGADOR_E  = 800_000_005L;
    private static final long JOGADOR_F  = 800_000_006L;
    private static final long JOGADOR_G  = 800_000_007L;
    private static final long JOGADOR_H  = 800_000_008L;
    private static final long CANAL      = 999L;

    @BeforeAll
    static void setupBanco() {
        Bd.iniciarBd();
        limparDadosDeTeste();
    }

    static void limparDadosDeTeste() {
        try (var conn = DriverManager.getConnection(URL)) {
            // Limpa batalhas ativas primeiro (sem FK para dono)
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM batalha_ativa WHERE desafiante_id >= 800000001 AND desafiante_id <= 800000010");
            }
            // Depois os donos (cascade apaga bicho/inventario)
            try (var pstmt = conn.prepareStatement(
                    "DELETE FROM dono WHERE id >= ? AND id <= ?")) {
                pstmt.setLong(1, 800_000_001L);
                pstmt.setLong(2, 800_000_010L);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Limpeza falhou: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Desafio cria batalha pendente com sucesso")
    void testDesafiarCriaBatalha() {
        tamagotchi.adotar(JOGADOR_A, "BichoA", "Leão");
        tamagotchi.adotar(JOGADOR_B, "BichoB", "Tigre");

        var resultado = batalha.desafiar(JOGADOR_A, JOGADOR_B, CANAL);

        assertTrue(resultado.sucesso(), "Desafio deve ter sucesso: " + resultado.mensagem());
        assertTrue(resultado.mensagem().contains("Desafio de batalha"), resultado.mensagem());
        assertTrue(resultado.batalhaId() > 0, "Deve ter ID de batalha");
    }

    @Test
    @DisplayName("Não pode desafiar a si mesmo")
    void testDesafiarASiMesmo() {
        var resultado = batalha.desafiar(JOGADOR_A, JOGADOR_A, CANAL);
        assertFalse(resultado.sucesso());
        assertTrue(resultado.mensagem().contains("não pode desafiar a si mesmo"), resultado.mensagem());
    }

    @Test
    @DisplayName("Aceitar desafio retorna mensagem de início da batalha")
    void testAceitarDesafio() {
        tamagotchi.adotar(JOGADOR_C, "Alpha", "Dragão");
        tamagotchi.adotar(JOGADOR_D, "Beta", "Fênix");

        var desafio = batalha.desafiar(JOGADOR_C, JOGADOR_D, CANAL);
        assertTrue(desafio.sucesso(), desafio.mensagem());

        var aceitou = batalha.aceitarDesafio(desafio.batalhaId(), JOGADOR_D);
        assertTrue(aceitou.contains("batalha") || aceitou.contains("turno"),
                "Aceite deve conter info de batalha: " + aceitou);
    }

    @Test
    @DisplayName("Recusar desafio encerra a batalha")
    void testRecusarDesafio() {
        tamagotchi.adotar(JOGADOR_E, "Corajoso", "Lobo");
        tamagotchi.adotar(JOGADOR_F, "Medroso", "Gato");

        var desafio = batalha.desafiar(JOGADOR_E, JOGADOR_F, CANAL);
        assertTrue(desafio.sucesso(), desafio.mensagem());

        var recusou = batalha.recusarDesafio(desafio.batalhaId(), JOGADOR_F);
        assertTrue(recusou.contains("recusou") || recusou.contains("Galinha"),
                "Recusa deve ter mensagem correta: " + recusou);
    }

    @Test
    @DisplayName("Desistir da batalha declara o oponente como vencedor")
    void testDesistir() {
        tamagotchi.adotar(JOGADOR_G, "Desistente", "Gato");
        tamagotchi.adotar(JOGADOR_H, "Vencedor", "Lobo");

        var desafio = batalha.desafiar(JOGADOR_G, JOGADOR_H, CANAL);
        assertTrue(desafio.sucesso(), desafio.mensagem());

        var aceitou = batalha.aceitarDesafio(desafio.batalhaId(), JOGADOR_H);
        assertTrue(aceitou.contains("batalha") || aceitou.contains("turno"),
                "Aceite deve iniciar batalha: " + aceitou);

        var desistiu = batalha.desistir(JOGADOR_G);
        assertTrue(desistiu.contains("desistiu"), "Desistência deve conter 'desistiu': " + desistiu);
        assertTrue(desistiu.contains("vence"), "Deve declarar vencedor: " + desistiu);
    }
}
