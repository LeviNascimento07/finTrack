package com.fintrack.controller;

import com.fintrack.dto.TransacaoRequestDTO;
import com.fintrack.dto.TransacaoResponseDTO;
import com.fintrack.exception.ErroResponse;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Transações", description = "CRUD de transações financeiras, com validação de saldo para despesas")
@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Operation(summary = "Listar transações do usuário",
            description = "Paginado; aceita filtros opcionais por categoria e por período (dataInicio/dataFim).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de transações"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} da rota não é o do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public Page<TransacaoResponseDTO> listar(@Parameter(description = "Id do usuário dono das transações", example = "1")
                                              @PathVariable Long usuarioId,
                                              @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                              @Parameter(description = "Filtra por categoria") @RequestParam(required = false) Long categoriaId,
                                              @Parameter(description = "Início do período (inclusive)", example = "2026-01-01")
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                              @Parameter(description = "Fim do período (inclusive)", example = "2026-01-31")
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                              @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.listar(usuarioId, categoriaId, dataInicio, dataFim, pageable);
    }

    @Operation(summary = "Buscar transação por id",
            description = "Transação de outro usuário retorna 404, igual a uma inexistente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação encontrada",
                    content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, ou transação inexistente/de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public TransacaoResponseDTO buscar(@Parameter(description = "Id do usuário dono da transação", example = "1")
                                        @PathVariable Long usuarioId,
                                        @Parameter(description = "Id da transação", example = "42")
                                        @PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.buscar(usuarioId, id);
    }

    @Operation(summary = "Criar transação",
            description = "A categoria informada precisa ser acessível ao usuário (global ou própria). "
                    + "Uma despesa que deixaria o saldo negativo é rejeitada (409).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transação criada",
                    content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campo ausente, valor não positivo, ou tipo inválido",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, ou categoria inacessível/inexistente",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Despesa deixaria o saldo negativo",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> criar(@Parameter(description = "Id do usuário dono da transação", example = "1")
                                                        @PathVariable Long usuarioId,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                                        @Valid @RequestBody TransacaoRequestDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        TransacaoResponseDTO criada = transacaoService.criar(usuarioId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @Operation(summary = "Atualizar transação",
            description = "Mesmas regras da criação: categoria acessível e saldo suficiente se for despesa "
                    + "— na comparação de saldo, o valor atual desta transação é desconsiderado antes de aplicar o novo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação atualizada",
                    content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campo ausente, valor não positivo, ou tipo inválido",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, transação ou categoria inexistente/inacessível",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Despesa deixaria o saldo negativo",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public TransacaoResponseDTO atualizar(@Parameter(description = "Id do usuário dono da transação", example = "1")
                                           @PathVariable Long usuarioId,
                                           @Parameter(description = "Id da transação", example = "42")
                                           @PathVariable Long id,
                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                           @Valid @RequestBody TransacaoRequestDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.atualizar(usuarioId, id, dto);
    }

    @Operation(summary = "Excluir transação")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transação excluída"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, ou transação inexistente/de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do usuário dono da transação", example = "1")
                                         @PathVariable Long usuarioId,
                                         @Parameter(description = "Id da transação", example = "42")
                                         @PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        transacaoService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }
}
