package top.leonam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemInventarioTest {

    private ItemInventario item;

    @BeforeEach
    void setUp() {
        item = new ItemInventario(123L, TipoItem.RACAO_COMUM, 5);
    }

    @Test
    @DisplayName("Deve adicionar e remover quantidade corretamente")
    void testManipularQuantidade() {
        item.adicionar(3);
        assertEquals(8, item.getQuantidade());

        item.remover(4);
        assertEquals(4, item.getQuantidade());

        assertThrows(IllegalStateException.class, () -> item.remover(10));
        assertThrows(IllegalArgumentException.class, () -> item.adicionar(-2));
    }

    @Test
    @DisplayName("Deve validar dados inválidos na criação")
    void testValidacoes() {
        assertThrows(IllegalArgumentException.class, () -> item.setDonoId(0));
        assertThrows(IllegalArgumentException.class, () -> item.setTipoItem(null));
        assertThrows(IllegalArgumentException.class, () -> item.setQuantidade(-1));
    }
}