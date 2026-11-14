package top.leonam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DonoTest {

    private Dono dono;

    @BeforeEach
    void setUp() {
        dono = new Dono(123456789L);
    }

    @Test
    @DisplayName("Deve inicializar com valores padrão corretos")
    void testInicializacao() {
        assertEquals(123456789L, dono.getId());
        assertEquals(100, dono.getMoedas());
        assertEquals(0, dono.getExperiencia());
        assertEquals(1, dono.getNivel());
        assertNotNull(dono.getCriadoEm());
    }

    @Test
    @DisplayName("Deve validar ID inválido")
    void testIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> dono.setId(0));
        assertThrows(IllegalArgumentException.class, () -> dono.setId(-10));
    }

    @Test
    @DisplayName("Deve adicionar e remover moedas corretamente")
    void testGerenciamentoMoedas() {
        dono.adicionarMoedas(50);
        assertEquals(150, dono.getMoedas());

        dono.removerMoedas(30);
        assertEquals(120, dono.getMoedas());

        // Deve falhar ao remover mais do que tem ou valores inválidos
        assertThrows(IllegalStateException.class, () -> dono.removerMoedas(500));
        assertThrows(IllegalArgumentException.class, () -> dono.adicionarMoedas(-10));
    }

    @Test
    @DisplayName("Deve somar experiência e subir de nível corretamente")
    void testExperienciaENivel() {
        assertEquals(1, dono.getNivel());

        dono.adicionarExperiencia(1000);
        assertEquals(1000, dono.getExperiencia());
        assertEquals(2, dono.getNivel());

        dono.adicionarExperiencia(1500);
        assertEquals(3, dono.getNivel());
    }
}