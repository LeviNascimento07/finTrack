package com.fintrack.dao;

import com.fintrack.model.Transacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoDAOTest {

    private Connection conn;
    private TransacaoDAO dao;

    @BeforeEach
    void setUp() {
        conn = Conexao.getConexaoTeste();
        dao = new TransacaoDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void inserirDeveGerarIdNoObjeto() {
        Transacao t = new Transacao("Salário", 3000.0, "RECEITA", LocalDate.of(2024, 1, 5));
        dao.inserir(t);
        assertTrue(t.getId() > 0, "id deve ser gerado pelo banco");
    }

    @Test
    void listarTodasDeveRetornarTodasAsInseridas() {
        dao.inserir(new Transacao("Salário",      3000.0, "RECEITA", LocalDate.of(2024, 1, 5)));
        dao.inserir(new Transacao("Conta de luz",  150.0, "DESPESA", LocalDate.of(2024, 1, 10)));

        List<Transacao> lista = dao.listarTodas();
        assertEquals(2, lista.size());
    }

    @Test
    void listarTodasDeveOrdenarPorDataDescendente() {
        dao.inserir(new Transacao("Antiga", 100.0, "RECEITA", LocalDate.of(2024, 1, 1)));
        dao.inserir(new Transacao("Recente", 200.0, "RECEITA", LocalDate.of(2024, 6, 1)));

        List<Transacao> lista = dao.listarTodas();
        assertTrue(lista.get(0).getData().isAfter(lista.get(1).getData()));
    }

    @Test
    void listarPorTipoDeveRetornarApenasOTipoSolicitado() {
        dao.inserir(new Transacao("Salário",     3000.0, "RECEITA", LocalDate.now()));
        dao.inserir(new Transacao("Aluguel",     1200.0, "DESPESA", LocalDate.now()));
        dao.inserir(new Transacao("Freelance",    800.0, "RECEITA", LocalDate.now()));

        List<Transacao> receitas = dao.listarPorTipo("RECEITA");
        assertEquals(2, receitas.size());
        receitas.forEach(t -> assertEquals("RECEITA", t.getTipo()));
    }

    @Test
    void atualizarDeveAlterarDadosNoBanco() {
        Transacao t = new Transacao("Original", 100.0, "RECEITA", LocalDate.of(2024, 1, 1));
        dao.inserir(t);

        t.setDescricao("Atualizado");
        t.setValor(250.0);
        dao.atualizar(t);

        Transacao salvo = dao.listarTodas().get(0);
        assertEquals("Atualizado", salvo.getDescricao());
        assertEquals(250.0, salvo.getValor());
    }

    @Test
    void deletarDeveRemoverRegistroDoBanco() {
        Transacao t = new Transacao("Para deletar", 50.0, "DESPESA", LocalDate.now());
        dao.inserir(t);
        dao.deletar(t.getId());

        assertTrue(dao.listarTodas().isEmpty());
    }

    @Test
    void somarPorTipoDeveRetornarSomaCorreta() {
        dao.inserir(new Transacao("A", 1000.0, "RECEITA", LocalDate.now()));
        dao.inserir(new Transacao("B",  500.0, "RECEITA", LocalDate.now()));
        dao.inserir(new Transacao("C",  200.0, "DESPESA", LocalDate.now()));

        assertEquals(1500.0, dao.somarPorTipo("RECEITA"), 0.001);
        assertEquals(200.0,  dao.somarPorTipo("DESPESA"), 0.001);
    }

    @Test
    void somarPorTipoSemRegistrosDeveRetornarZero() {
        assertEquals(0.0, dao.somarPorTipo("RECEITA"), 0.001);
    }
}