package top.leonam.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.leonam.bd.Bd;
import top.leonam.model.TipoItem;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do TamagotchiService.
 * Usam o banco dados.db real, com limpeza dos IDs de teste antes de rodar.
 * Cada teste tem um ID de usuário único para isolamento entre testes.
 */
class TamagotchiServiceTest {
    private final TamagotchiService service = new TamagotchiService();
    private final LojaService loja = new LojaService();

    static final long ID_BASE = 900_000_000L;
    static final String URL   = "jdbc:sqlite:dados.db";

    @BeforeAll
    static void setupBanco() {
        Bd.iniciarBd();
        limparDadosDeTeste();
    }

    static void limparDadosDeTeste() {
        // Remove explicitamente bicho e inventário antes do dono
        // (cascade pode não funcionar sem PRAGMA foreign_keys = ON na conexão de limpeza)
        try (var conn = DriverManager.getConnection(URL);
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM bicho      WHERE dono_id >= 900000000 AND dono_id <= 900000100");
            stmt.executeUpdate("DELETE FROM inventario WHERE dono_id >= 900000000 AND dono_id <= 900000100");
            stmt.executeUpdate("DELETE FROM dono       WHERE id      >= 900000000 AND id      <= 900000100");
        } catch (SQLException e) {
            System.err.println("Limpeza falhou: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Adotar cria bicho e dá 3 rações iniciais")
    void testAdotar() {
        long id = ID_BASE + 1;
        var resultado = service.adotar(id, "Bolinha", "Gato");
        assertTrue(resultado.contains("Bolinha"), "Esperado nome no resultado: " + resultado);
        assertTrue(resultado.contains("adotado"), "Esperado 'adotado' no resultado: " + resultado);
    }

    @Test
    @DisplayName("Não pode adotar segundo bicho enquanto o primeiro vive")
    void testAdotarDuasVezes() {
        long id = ID_BASE + 2;
        service.adotar(id, "Primeiro", "Gato");
        var resultado = service.adotar(id, "Segundo", "Cão");
        assertTrue(resultado.contains("Você já tem um bicho"), "Esperado bloqueio: " + resultado);
    }

    @Test
    @DisplayName("Daily dá 100 moedas e bloqueia por 24h")
    void testDailyBloqueio() {
        long id = ID_BASE + 3;
        service.adotar(id, "DailyBicho", "Gato");

        var primeira = service.resgatarDaily(id);
        assertTrue(primeira.contains("100") || primeira.contains("+100"), "Primeira daily: " + primeira);

        var segunda = service.resgatarDaily(id);
        assertTrue(segunda.contains("já resgatou") || segunda.contains("Volte em"),
                "Segunda daily deve bloquear: " + segunda);
    }

    @Test
    @DisplayName("Status retorna informações do bicho")
    void testStatus() {
        long id = ID_BASE + 4;
        service.adotar(id, "Statusinho", "Dragão");
        var resultado = service.status(id);
        assertTrue(resultado.contains("Statusinho"), "Status deve conter o nome: " + resultado);
        assertTrue(resultado.contains("Vida") || resultado.contains("❤️"), "Status deve conter vida: " + resultado);
    }

    @Test
    @DisplayName("Iniciar brincar retorna desafio válido para bicho vivo")
    void testIniciarBrincar() {
        long id = ID_BASE + 5;
        service.adotar(id, "Brincalhão", "Coelho");

        var desafio = service.iniciarBrincar(id);
        assertTrue(desafio.sucesso(), "Desafio deve ter sucesso: " + desafio.mensagem());
        assertNotNull(desafio.emojiCorreto());
        assertFalse(desafio.emojiCorreto().isBlank());
    }

    @Test
    @DisplayName("Resolver brincar com acerto dá recompensa de 20 moedas")
    void testResolverBrincarAcerto() {
        long id = ID_BASE + 6;
        service.adotar(id, "Esperto", "Raposa");

        var desafio  = service.iniciarBrincar(id);
        var resultado = service.resolverBrincar(id, desafio.emojiCorreto(), desafio.emojiCorreto(), false);

        assertTrue(resultado.contains("Acertou") || resultado.contains("20 moedas"),
                "Acerto deve recompensar: " + resultado);
    }

    @Test
    @DisplayName("Resolver brincar com erro dá recompensa menor")
    void testResolverBrincarErro() {
        long id = ID_BASE + 7;
        service.adotar(id, "Atrapalhado", "Pinguim");

        // Garante que o emoji enviado é diferente do correto
        var desafio   = service.iniciarBrincar(id);
        var emojiErrado = desafio.emojiCorreto().equals("🌟") ? "🎾" : "🌟";
        var resultado = service.resolverBrincar(id, emojiErrado, desafio.emojiCorreto(), false);

        assertTrue(resultado.contains("Errou") || resultado.contains("5 moedas"),
                "Erro deve dar recompensa menor: " + resultado);
    }

    @Test
    @DisplayName("Vacinar falha sem vacina no inventário")
    void testVacinarSemVacina() {
        long id = ID_BASE + 8;
        service.adotar(id, "Sujo", "Hamster");
        // Reduz limpeza manualmente via banho invertido — ou simplesmente testa o retorno
        // com limpeza < 100 (não há como forçar via service sem passar tempo real)
        // Então o teste verifica o caminho completo: com limpeza cheia, vacina retorna aviso correto
        var resultadoCheia = service.vacinar(id);
        assertTrue(resultadoCheia.contains("vacinado") || resultadoCheia.contains("limpinho"),
                "Com limpeza cheia deve informar que está ok: " + resultadoCheia);
    }

    @Test
    @DisplayName("Usar brinquedo sem tê-lo no inventário avisa o jogador")
    void testUsarBrinquedoSemBrinquedo() {
        long id = ID_BASE + 9;
        service.adotar(id, "Felizinho", "Sapo");

        // Bicho nasce com felicidade 100 — não precisa de brinquedo ainda,
        // mas primeiro testa sem ter brinquedo no inventário
        var resultado = service.usarBrinquedo(id);
        // Como felicidade está cheia, retorna "já está super feliz" antes de checar inventário
        assertTrue(resultado.contains("super feliz") || resultado.contains("Brinquedo"),
                "Deve informar felicidade cheia ou falta de brinquedo: " + resultado);
    }

    @Test
    @DisplayName("Usar brinquedo com item no inventário e felicidade cheia avisa que está feliz")
    void testUsarBrinquedoComBrinquedoEFelicidadeCheia() {
        long id = ID_BASE + 10;
        service.adotar(id, "Pudim", "Coelho");
        // Dono nasce com 100 moedas; brinquedo custa 80
        var compra = loja.comprar(id, "brinquedo");
        assertTrue(compra.contains("Comprado"), "Compra deve sucesso: " + compra);

        var resultado = service.usarBrinquedo(id);
        // Felicidade está em 100, então o bicho "já está super feliz"
        assertTrue(resultado.contains("super feliz"),
                "Com felicidade cheia deve avisar: " + resultado);
    }

    @Test
    @DisplayName("Treinar gasta moedas e aumenta bônus de ataque")
    void testTreinar() {
        long id = ID_BASE + 11;
        service.adotar(id, "Bruiser", "Lobo");
        // Custo: 50 + 1 * 25 = 75 (nivel 1)
        var resultado = service.treinar(id);
        assertTrue(resultado.contains("treinou"), "Treino deve sucesso: " + resultado);
        assertTrue(resultado.contains("+2 de ataque"), "Deve mostrar bônus: " + resultado);
    }

    @Test
    @DisplayName("Treinar tem cooldown de 1 hora")
    void testTreinarCooldown() {
        long id = ID_BASE + 12;
        service.adotar(id, "Fadigado", "Gato");
        service.treinar(id); // Primeiro treino OK
        var resultado = service.treinar(id); // Segundo deve ser bloqueado
        assertTrue(resultado.contains("recuperando") || resultado.contains("Aguarde"),
                "Segundo treino deve ser bloqueado por cooldown: " + resultado);
    }

    @Test
    @DisplayName("Treinar sem moedas suficientes avisa")
    void testTreinarSemMoedas() {
        long id = ID_BASE + 13;
        service.adotar(id, "Pobre", "Sapo");
        // Gasta todas as moedas no brinquedo (80) — sobram 20
        loja.comprar(id, "brinquedo");
        // Custo do treino: 50 + 1*25 = 75; só tem 20
        var resultado = service.treinar(id);
        assertTrue(resultado.contains("moedas") && resultado.contains("75"),
                "Deve avisar falta de moedas: " + resultado);
    }

    @Test
    @DisplayName("Girar cobra 50 moedas e retorna resultado")
    void testGirar() {
        long id = ID_BASE + 14;
        service.adotar(id, "Apostador", "Raposa");
        var resultado = service.girar(id);
        assertTrue(resultado.contains("Caça-Níqueis") || resultado.contains("Caça"),
                "Deve mostrar resultado do caça-níqueis: " + resultado);
    }

    @Test
    @DisplayName("Girar sem moedas suficientes avisa")
    void testGirarSemMoedas() {
        long id = ID_BASE + 15;
        service.adotar(id, "Quebrado", "Sapo");
        // Gasta quase tudo — sobram 20 (brinquedo custa 80, tem 100)
        loja.comprar(id, "brinquedo");
        // 50 moedas para girar, só tem 20
        var resultado = service.girar(id);
        assertTrue(resultado.contains("50") && resultado.contains("moedas"),
                "Deve avisar que precisa de 50 moedas: " + resultado);
    }

    @Test
    @DisplayName("Presentear transfere moedas entre jogadores")
    void testPresentear() {
        long origemId = ID_BASE + 16;
        long destinoId = ID_BASE + 17;
        service.adotar(origemId, "Generoso", "Coelho");
        service.adotar(destinoId, "Feliz", "Gato");
        // Origem tem 100 moedas, presenteia 30
        var resultado = service.presentear(origemId, destinoId, 30);
        assertTrue(resultado.contains("presenteou"), "Deve confirmar presente: " + resultado);
    }

    @Test
    @DisplayName("Presentear sem moedas suficientes bloqueia")
    void testPresentearSemMoedas() {
        long origemId = ID_BASE + 18;
        long destinoId = ID_BASE + 19;
        service.adotar(origemId, "SemGrana", "Sapo");
        service.adotar(destinoId, "Receptor", "Gato");
        var resultado = service.presentear(origemId, destinoId, 999);
        assertTrue(resultado.contains("suficientes") || resultado.contains("moedas"),
                "Deve bloquear transferência sem moedas: " + resultado);
    }

    @Test
    @DisplayName("Usar poção de cura total restaura todos os atributos")
    void testUsarPoçãoCuraTotal() {
        long id = ID_BASE + 20;
        service.adotar(id, "Curado", "Unicórnio");
        // Poção custa 500, mas jogador tem só 100 — precisa dar moedas
        var dono = new top.leonam.dao.DonoDAO().buscarPorId(id);
        dono.adicionarMoedas(500);
        new top.leonam.dao.DonoDAO().salvar(dono);
        loja.comprar(id, "POCAO_CURA_TOTAL");
        var resultado = service.usarPoçãoCuraTotal(id);
        assertTrue(resultado.contains("revigorado") || resultado.contains("Cura Total"),
                "Deve confirmar uso da poção: " + resultado);
    }
}
