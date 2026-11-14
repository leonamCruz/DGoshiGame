package top.leonam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BichoTest {

    private Bicho bicho;

    @BeforeEach
    void setUp() {
        bicho = new Bicho();
        bicho.setNome("Rex");
        bicho.setFome(100);
        bicho.setVida(100);
        bicho.setLimpeza(100);
        bicho.setFelicidade(100);
        bicho.setNascimento(OffsetDateTime.now());
        bicho.setUltimaInteracao(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Deve aceitar nome válido e rejeitar nome inválido")
    void testNomeValidation() {
        bicho.setNome("Bobby");
        assertEquals("Bobby", bicho.getNome());

        assertThrows(IllegalArgumentException.class, () -> bicho.setNome("A"));
        assertThrows(IllegalArgumentException.class, () -> bicho.setNome(null));
    }

    @Test
    @DisplayName("Deve respeitar os limites de status (0 a 100)")
    void testStatusLimits() {
        bicho.setFome(50);
        assertEquals(50, bicho.getFome());

        assertThrows(IllegalArgumentException.class, () -> bicho.setFome(101));
        assertThrows(IllegalArgumentException.class, () -> bicho.setFome(-1));

        assertThrows(IllegalArgumentException.class, () -> bicho.setFelicidade(101));
        assertThrows(IllegalArgumentException.class, () -> bicho.setFelicidade(-1));
    }

    @Test
    @DisplayName("Deve reduzir a fome com base no tempo decorrido")
    void testDecaimentoFome() {
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(3));
        bicho.setFome(100);

        bicho.atualizarStatusPeloTempo();

        // 3 horas * 5/hora = 15 perdidos → 85
        assertEquals(85, bicho.getFome());
        assertFalse(bicho.isMorto());
    }

    @Test
    @DisplayName("Deve reduzir limpeza com base no tempo decorrido")
    void testDecaimentoLimpeza() {
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(10));
        bicho.setLimpeza(100);

        bicho.atualizarStatusPeloTempo();

        // 10 horas * 2/hora = 20 perdidos → 80
        assertEquals(80, bicho.getLimpeza());
    }

    @Test
    @DisplayName("Deve reduzir felicidade com base no tempo decorrido (mínimo 10)")
    void testDecaimentoFelicidade() {
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(20));
        bicho.setFelicidade(100);

        bicho.atualizarStatusPeloTempo();

        // 20 horas * 1/hora = 20 perdidos → 80
        assertEquals(80, bicho.getFelicidade());

        // Teste do piso mínimo de 10
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(200));
        bicho.setFelicidade(100);
        bicho.atualizarStatusPeloTempo();
        assertEquals(10, bicho.getFelicidade());
    }

    @Test
    @DisplayName("Deve perder vida quando fome chega a zero")
    void testPerdaVidaPorFome() {
        // Fome já zerada + 5 horas sem interação
        bicho.setFome(0);
        bicho.setVida(100);
        bicho.setLimpeza(100);
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(5));

        bicho.atualizarStatusPeloTempo();

        // 5 horas com fome = 0 → perde 3 vida/hora = 15 de dano → vida = 85
        assertEquals(85, bicho.getVida());
    }

    @Test
    @DisplayName("Deve marcar o bicho como morto quando vida chega a zero")
    void testMorte() {
        bicho.setVida(10);
        bicho.setFome(0);
        bicho.setLimpeza(0);
        bicho.setUltimaInteracao(OffsetDateTime.now().minusHours(10));

        bicho.atualizarStatusPeloTempo();

        assertTrue(bicho.isMorto());
        assertEquals(0, bicho.getVida());
    }

    @Test
    @DisplayName("Deve calcular estágio correto com base na idade")
    void testEstagios() {
        // Recém-nascido = Ovo
        bicho.setNascimento(OffsetDateTime.now().minusHours(6));
        assertEquals("Ovo", bicho.calcularEstagio());

        // 3 dias = Filhote
        bicho.setNascimento(OffsetDateTime.now().minusDays(3));
        assertEquals("Filhote", bicho.calcularEstagio());

        // 15 dias = Jovem
        bicho.setNascimento(OffsetDateTime.now().minusDays(15));
        assertEquals("Jovem", bicho.calcularEstagio());

        // 45 dias = Adulto
        bicho.setNascimento(OffsetDateTime.now().minusDays(45));
        assertEquals("Adulto", bicho.calcularEstagio());
    }

    @Test
    @DisplayName("Ataque base deve aumentar com o estágio")
    void testAtaqueBase() {
        // Ovo/Filhote: sem bonus de estágio, +5 de felicidade (100 >= 70)
        bicho.setNascimento(OffsetDateTime.now());
        int ataqueOvo = bicho.calcularAtaqueBase();
        assertTrue(ataqueOvo >= 15 && ataqueOvo <= 29,
                "Ovo deve ter ataque 15-29 (10 base + 5 felicidade + 0-14 random): " + ataqueOvo);

        // Adulto: bonus +15 de estágio + 5 de felicidade
        bicho.setNascimento(OffsetDateTime.now().minusDays(45));
        int ataqueAdulto = bicho.calcularAtaqueBase();
        assertTrue(ataqueAdulto >= 30 && ataqueAdulto <= 44,
                "Adulto deve ter ataque 30-44 (10 base + 15 estágio + 5 felicidade + 0-14 random): " + ataqueAdulto);
    }

    @Test
    @DisplayName("Bônus de treino deve aumentar o ataque base")
    void testAtaqueBaseComTreino() {
        bicho.setNascimento(OffsetDateTime.now());
        bicho.setBonusAtaqueTreino(10);

        // Com +10 de treino: 10 base + 5 felicidade + 10 treino + 0-14 random = 25-39
        int ataque = bicho.calcularAtaqueBase();
        assertTrue(ataque >= 25 && ataque <= 39,
                "Com treino +10 deve ter ataque 25-39: " + ataque);
    }
}
