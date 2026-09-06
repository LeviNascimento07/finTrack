package com.fintrack.repository;

import com.fintrack.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("select c from Categoria c where c.usuario is null or c.usuario.id = :usuarioId")
    List<Categoria> findVisiveisPara(@Param("usuarioId") Long usuarioId);

    @Query("select c from Categoria c where c.id = :id and (c.usuario is null or c.usuario.id = :usuarioId)")
    Optional<Categoria> findAcessivelPor(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    boolean existsByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);

    boolean existsByNomeIgnoreCaseAndUsuarioIsNull(String nome);
}
