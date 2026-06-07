package com.fintrack.generic;

import com.fintrack.model.Transacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioGenericoTest {

    private RepositorioGenerico<Transacao> repo;

    @BeforeEach
    void setUp() {
        repo = new RepositorioGenerico<>();
    }

    @AfterEach
    void tearDown() {
        repo.limpar();
    }

    @Test
    void adicionarDeveIncrementarTamanho() {
        repo.adicionar(new Transacao("Teste", 10.0, "RECEITA", LocalDate.now()));
        assertEquals(1, repo.tamanho());
    }

    @Test
    void removerDeveDecrementarTamanho() {
        Transacao t = new Transacao("Teste", 10.0, "RECEITA", LocalDate.now());
        repo.adicionar(t);
        repo.remover(t);
        assertEquals(0, repo.tamanho());
    }

    @Test
    void listarTodosDeveRetornarCopiaDefensiva() {
        repo.adicionar(new Transacao("Teste", 10.0, "RECEITA", LocalDate.now()));
        List<Transacao> copia = repo.listarTodos();
        copia.clear();
        assertEquals(1, repo.tamanho(), "lista original não deve ser afetada");
    }

    @Test
    void adicionarTodosDeveUsarWildcardExtendsT() {
        List<Transacao> lista = List.of(
                new Transacao("A", 100.0, "RECEITA", LocalDate.now()),
                new Transacao("B", 200.0, "DESPESA", LocalDate.now())
        );
        repo.adicionarTodos(lista);
        assertEquals(2, repo.tamanho());
    }

    @Test
    void copiarParaDeveUsarWildcardSuperT() {
        repo.adicionar(new Transacao("X", 50.0, "RECEITA", LocalDate.now()));
        List<Object> destino = new ArrayList<>();
        repo.copiarPara(destino);
        assertEquals(1, destino.size());
    }

    @Test
    void exibirTodosDeveAceitarRepositorioDeQualquerTipo() {
        RepositorioGenerico<String> repoStr = new RepositorioGenerico<>();
        repoStr.adicionar("item1");
        repoStr.adicionar("item2");
        assertDoesNotThrow(() -> RepositorioGenerico.exibirTodos(repoStr));
    }

    @Test
    void limparDeveEsvaziarORepositorio() {
        repo.adicionar(new Transacao("X", 50.0, "RECEITA", LocalDate.now()));
        repo.limpar();
        assertEquals(0, repo.tamanho());
    }
}