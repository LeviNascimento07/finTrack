package com.fintrack.repository;

import com.fintrack.model.TipoTransacao;
import com.fintrack.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Regra de ouro: nenhum metodo de transacao sem usuarioId no filtro.
 * O isolamento entre usuarios existe na camada de dados, nao so na de seguranca.
 */
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByIdAndUsuarioId(Long id, Long usuarioId);

    Page<Transacao> findByUsuarioIdOrderByDataDesc(Long usuarioId, Pageable pageable);

    /**
     * Filtros opcionais (categoriaId, inicio, fim) para o GET paginado do
     * TransacaoController. O ordenamento vem do Pageable, nao daqui.
     */
    @Query("""
            select t from Transacao t
            where t.usuario.id = :usuarioId
              and (:categoriaId is null or t.categoria.id = :categoriaId)
              and (:inicio is null or t.data >= :inicio)
              and (:fim is null or t.data <= :fim)
            """)
    Page<Transacao> findComFiltros(@Param("usuarioId") Long usuarioId,
                                    @Param("categoriaId") Long categoriaId,
                                    @Param("inicio") LocalDate inicio,
                                    @Param("fim") LocalDate fim,
                                    Pageable pageable);

    List<Transacao> findByUsuarioIdAndDataBetweenOrderByDataDesc(Long usuarioId, LocalDate inicio, LocalDate fim);

    List<Transacao> findByUsuarioIdAndCategoriaIdOrderByDataDesc(Long usuarioId, Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
            select coalesce(sum(t.valor), 0) from Transacao t
            where t.usuario.id = :usuarioId and t.tipo = :tipo
            """)
    BigDecimal sumValorByUsuarioIdAndTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTransacao tipo);

    @Query("""
            select coalesce(sum(t.valor), 0) from Transacao t
            where t.usuario.id = :usuarioId and t.tipo = :tipo
              and t.data between :inicio and :fim
            """)
    BigDecimal sumValorByUsuarioIdAndTipoAndDataBetween(@Param("usuarioId") Long usuarioId,
                                                          @Param("tipo") TipoTransacao tipo,
                                                          @Param("inicio") LocalDate inicio,
                                                          @Param("fim") LocalDate fim);

    @Query("""
            select c.nome as nomeCategoria, coalesce(sum(t.valor), 0) as total
            from Transacao t join t.categoria c
            where t.usuario.id = :usuarioId and t.tipo = :tipo
            group by c.nome
            order by c.nome
            """)
    List<TotalPorCategoria> totalizarPorCategoria(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTransacao tipo);
}
