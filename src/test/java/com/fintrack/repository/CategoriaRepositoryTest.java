package com.fintrack.repository;

import com.fintrack.model.Categoria;
import com.fintrack.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

/**
 * @DataJpaTest troca o datasource por um banco embarcado por padrao, que
 * nao existe neste projeto (so SQLite) — Replace.NONE mantem o datasource
 * de src/test/resources/application.yml. Cada metodo roda numa transacao
 * que da rollback ao final (padrao do @DataJpaTest), entao os dados de um
 * teste nunca vazam para outro.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario novoUsuario() {
        return usuarioRepository.save(
                new Usuario("Usuário " + UUID.randomUUID(), UUID.randomUUID() + "@teste.com", "hash"));
    }

    @Test
    void findVisiveisPara_trazGlobaisEDoUsuario_naoTrazDeOutroUsuario() {
        Usuario usuarioA = novoUsuario();
        Usuario usuarioB = novoUsuario();

        Categoria global = categoriaRepository.save(new Categoria("Global-" + UUID.randomUUID(), null));
        Categoria propriaA = categoriaRepository.save(new Categoria("PropriaA-" + UUID.randomUUID(), usuarioA));
        Categoria propriaB = categoriaRepository.save(new Categoria("PropriaB-" + UUID.randomUUID(), usuarioB));

        List<Categoria> visiveis = categoriaRepository.findVisiveisPara(usuarioA.getId());

        assertThat(visiveis).contains(global, propriaA);
        assertThat(visiveis).doesNotContain(propriaB);
    }

    @Test
    void findAcessivelPor_categoriaDeOutroUsuario_retornaVazio() {
        Usuario usuarioA = novoUsuario();
        Usuario usuarioB = novoUsuario();
        Categoria propriaB = categoriaRepository.save(new Categoria("PropriaB-" + UUID.randomUUID(), usuarioB));

        Optional<Categoria> resultado = categoriaRepository.findAcessivelPor(propriaB.getId(), usuarioA.getId());

        assertThat(resultado).isEmpty();
    }

    @Test
    void findAcessivelPor_globalOuPropria_retornaPresente() {
        Usuario usuarioA = novoUsuario();
        Categoria global = categoriaRepository.save(new Categoria("Global-" + UUID.randomUUID(), null));
        Categoria propriaA = categoriaRepository.save(new Categoria("PropriaA-" + UUID.randomUUID(), usuarioA));

        assertThat(categoriaRepository.findAcessivelPor(global.getId(), usuarioA.getId())).isPresent();
        assertThat(categoriaRepository.findAcessivelPor(propriaA.getId(), usuarioA.getId())).isPresent();
    }
}
