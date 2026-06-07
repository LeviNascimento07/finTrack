package com.fintrack.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoTest {

    @Test
    void construtorCompletoDevePreencherTodosOsCampos() {
        LocalDate data = LocalDate.of(2024, 1, 15);
        Transacao t = new Transacao(1, "Salário", 3000.0, "RECEITA", data);

        assertEquals(1, t.getId());
        assertEquals("Salário", t.getDescricao());
        assertEquals(3000.0, t.getValor());
        assertEquals("RECEITA", t.getTipo());
        assertEquals(data, t.getData());
    }

    @Test
    void construtorSemIdDeveManterIdZero() {
        Transacao t = new Transacao("Aluguel", 1500.0, "DESPESA", LocalDate.now());
        assertEquals(0, t.getId());
    }

    @Test
    void construtorVazioDevePermitirPreenchimentoViaSetter() {
        Transacao t = new Transacao();
        t.setId(5);
        t.setDescricao("Teste");
        t.setValor(100.0);
        t.setTipo("RECEITA");
        t.setData(LocalDate.of(2024, 6, 1));

        assertEquals(5, t.getId());
        assertEquals("Teste", t.getDescricao());
        assertEquals(100.0, t.getValor());
        assertEquals("RECEITA", t.getTipo());
        assertEquals(LocalDate.of(2024, 6, 1), t.getData());
    }

    @Test
    void toStringDeveConterOsPrincipaisCampos() {
        Transacao t = new Transacao(2, "Conta de luz", 150.0, "DESPESA", LocalDate.of(2024, 3, 10));
        String s = t.toString();

        assertTrue(s.contains("2"));
        assertTrue(s.contains("Conta de luz"));
        assertTrue(s.contains("150.0"));
        assertTrue(s.contains("DESPESA"));
    }
}