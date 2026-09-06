package com.fintrack.repository;

import com.fintrack.model.Categoria;
import com.fintrack.model.TipoTransacao;
import com.fintrack.model.Transacao;
import com.fintrack.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

/**
 * @DataJpaTest troca o datasource por um banco embarcado por padrao, que
 * nao existe neste projeto (so SQLite) — Replace.NONE mantem o datasource
 * de src/test/resources/application.yml.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TransacaoRepositoryTest {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario novoUsuario() {
        return usuarioRepository.save(
                new Usuario("Usuário " + UUID.randomUUID(), UUID.randomUUID() + "@teste.com", "hash"));
    }

    private Categoria novaCategoria(String nome, Usuario usuario) {
        return categoriaRepository.save(new Categoria(nome, usuario));
    }

    private Transacao novaTransacao(Usuario usuario, Categoria categoria, TipoTransacao tipo, String valor, LocalDate data) {
        return transacaoRepository.save(new Transacao("desc", new BigDecimal(valor), tipo, data, usuario, categoria));
    }

    @Test
    void sumValorByUsuarioIdAndTipo_somaApenasTransacoesDoTipoESeuUsuario() {
        Usuario usuario = novoUsuario();
        Usuario outroUsuario = novoUsuario();
        Categoria categoria = novaCategoria("Cat", usuario);
        Categoria categoriaOutro = novaCategoria("CatOutro", outroUsuario);

        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "100.00", LocalDate.of(2026, 1, 1));
        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "50.00", LocalDate.of(2026, 1, 2));
        novaTransacao(usuario, categoria, TipoTransacao.RECEITA, "999.00", LocalDate.of(2026, 1, 3));
        novaTransacao(outroUsuario, categoriaOutro, TipoTransacao.DESPESA, "500.00", LocalDate.of(2026, 1, 1));

        BigDecimal total = transacaoRepository.sumValorByUsuarioIdAndTipo(usuario.getId(), TipoTransacao.DESPESA);

        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    void sumValorByUsuarioIdAndTipo_usuarioSemTransacoes_retornaZeroNuncaNull() {
        Usuario usuario = novoUsuario();

        BigDecimal total = transacaoRepository.sumValorByUsuarioIdAndTipo(usuario.getId(), TipoTransacao.DESPESA);

        assertThat(total).isNotNull();
        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    void sumValorByUsuarioIdAndTipoAndDataBetween_bordasDoIntervaloSaoInclusivas() {
        Usuario usuario = novoUsuario();
        Categoria categoria = novaCategoria("Cat", usuario);
        LocalDate inicio = LocalDate.of(2026, 1, 10);
        LocalDate fim = LocalDate.of(2026, 1, 20);

        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "10.00", inicio.minusDays(1));
        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "20.00", inicio);
        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "30.00", fim);
        novaTransacao(usuario, categoria, TipoTransacao.DESPESA, "40.00", fim.plusDays(1));

        BigDecimal total = transacaoRepository.sumValorByUsuarioIdAndTipoAndDataBetween(
                usuario.getId(), TipoTransacao.DESPESA, inicio, fim);

        assertThat(total).isEqualByComparingTo("50.00");
    }

    @Test
    void totalizarPorCategoria_agrupaEOrdenaPorNome() {
        Usuario usuario = novoUsuario();
        Categoria zebra = novaCategoria("Zebra", usuario);
        Categoria alimentacao = novaCategoria("Alimentação", usuario);
        Categoria moda = novaCategoria("Moda", usuario);

        novaTransacao(usuario, zebra, TipoTransacao.DESPESA, "10.00", LocalDate.of(2026, 1, 1));
        novaTransacao(usuario, alimentacao, TipoTransacao.DESPESA, "20.00", LocalDate.of(2026, 1, 2));
        novaTransacao(usuario, alimentacao, TipoTransacao.DESPESA, "5.00", LocalDate.of(2026, 1, 3));
        novaTransacao(usuario, moda, TipoTransacao.DESPESA, "7.00", LocalDate.of(2026, 1, 4));
        novaTransacao(usuario, zebra, TipoTransacao.RECEITA, "1000.00", LocalDate.of(2026, 1, 5));

        List<TotalPorCategoria> totais = transacaoRepository.totalizarPorCategoria(usuario.getId(), TipoTransacao.DESPESA);

        assertThat(totais).hasSize(3);
        assertThat(totais.get(0).getNomeCategoria()).isEqualTo("Alimentação");
        assertThat(totais.get(0).getTotal()).isEqualByComparingTo("25.00");
        assertThat(totais.get(1).getNomeCategoria()).isEqualTo("Moda");
        assertThat(totais.get(1).getTotal()).isEqualByComparingTo("7.00");
        assertThat(totais.get(2).getNomeCategoria()).isEqualTo("Zebra");
        assertThat(totais.get(2).getTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void findComFiltros_categoriaEPeriodo_isoladosECombinados() {
        Usuario usuario = novoUsuario();
        Usuario outroUsuario = novoUsuario();
        Categoria catX = novaCategoria("CategoriaX", usuario);
        Categoria catY = novaCategoria("CategoriaY", usuario);
        Categoria catOutro = novaCategoria("CategoriaOutro", outroUsuario);

        Transacao dentroXDentroData = novaTransacao(usuario, catX, TipoTransacao.DESPESA, "10.00", LocalDate.of(2026, 1, 15));
        Transacao dentroXForaData = novaTransacao(usuario, catX, TipoTransacao.DESPESA, "20.00", LocalDate.of(2026, 3, 1));
        Transacao dentroYDentroData = novaTransacao(usuario, catY, TipoTransacao.DESPESA, "30.00", LocalDate.of(2026, 1, 20));
        novaTransacao(outroUsuario, catOutro, TipoTransacao.DESPESA, "999.00", LocalDate.of(2026, 1, 15));

        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        Pageable pageable = PageRequest.of(0, 20);

        Page<Transacao> semFiltro = transacaoRepository.findComFiltros(usuario.getId(), null, null, null, pageable);
        assertThat(semFiltro.getContent()).containsExactlyInAnyOrder(dentroXDentroData, dentroXForaData, dentroYDentroData);

        Page<Transacao> soCategoria = transacaoRepository.findComFiltros(usuario.getId(), catX.getId(), null, null, pageable);
        assertThat(soCategoria.getContent()).containsExactlyInAnyOrder(dentroXDentroData, dentroXForaData);

        Page<Transacao> soPeriodo = transacaoRepository.findComFiltros(usuario.getId(), null, inicio, fim, pageable);
        assertThat(soPeriodo.getContent()).containsExactlyInAnyOrder(dentroXDentroData, dentroYDentroData);

        Page<Transacao> categoriaEPeriodo = transacaoRepository.findComFiltros(usuario.getId(), catX.getId(), inicio, fim, pageable);
        assertThat(categoriaEPeriodo.getContent()).containsExactly(dentroXDentroData);
    }
}
