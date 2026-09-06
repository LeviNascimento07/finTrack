package com.fintrack.config;

import com.fintrack.model.Categoria;
import com.fintrack.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Semeia as categorias globais na primeira execucao. Idempotente: cada
 * nome so e inserido se ainda nao existir como categoria global, entao
 * rodar de novo (ex: apos apagar fintrack.db) nao duplica nada.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final List<String> CATEGORIAS_GLOBAIS = List.of(
            "Alimentação", "Transporte", "Moradia", "Saúde", "Educação", "Lazer", "Salário", "Outros"
    );

    private final CategoriaRepository categoriaRepository;

    public DataInitializer(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(String... args) {
        for (String nome : CATEGORIAS_GLOBAIS) {
            if (!categoriaRepository.existsByNomeIgnoreCaseAndUsuarioIsNull(nome)) {
                categoriaRepository.save(new Categoria(nome, null));
            }
        }
    }
}
