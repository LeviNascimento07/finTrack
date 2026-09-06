package com.fintrack.controller;

import com.fintrack.dto.SaldoDTO;
import com.fintrack.dto.TotalPorCategoriaDTO;
import com.fintrack.exception.ErroResponse;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.model.TipoTransacao;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Saldo e Relatórios", description = "Saldo geral/por período e distribuição de transações por categoria")
@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @Operation(summary = "Consultar saldo",
            description = "Sem parâmetros, retorna o saldo geral (todas as transações, sem recorte de data). "
                    + "Informando dataInicio e dataFim — os dois juntos, ou nenhum — retorna o saldo do período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo calculado",
                    content = @Content(schema = @Schema(implementation = SaldoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Apenas uma das datas informada, ou dataInicio posterior a dataFim",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} da rota não é o do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/saldo")
    public SaldoDTO saldo(@Parameter(description = "Id do usuário", example = "1")
                           @PathVariable Long usuarioId,
                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                           @Parameter(description = "Início do período (obrigatório junto com dataFim)", example = "2026-01-01")
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                           @Parameter(description = "Fim do período (obrigatório junto com dataInicio)", example = "2026-01-31")
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return relatorioService.calcularSaldo(usuarioId, dataInicio, dataFim);
    }

    @Operation(summary = "Totalizar transações por categoria",
            description = "Soma os valores do tipo informado (RECEITA ou DESPESA, default DESPESA), agrupados por categoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de totais por categoria",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TotalPorCategoriaDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Valor de 'tipo' inválido (deve ser RECEITA ou DESPESA)",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} da rota não é o do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/relatorios/por-categoria")
    public List<TotalPorCategoriaDTO> porCategoria(@Parameter(description = "Id do usuário", example = "1")
                                                     @PathVariable Long usuarioId,
                                                     @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                                     @Parameter(description = "RECEITA ou DESPESA", example = "DESPESA")
                                                     @RequestParam(defaultValue = "DESPESA") TipoTransacao tipo) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return relatorioService.totalizarPorCategoria(usuarioId, tipo);
    }

    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }
}
