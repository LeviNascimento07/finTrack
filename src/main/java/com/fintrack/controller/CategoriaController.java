package com.fintrack.controller;

import com.fintrack.dto.CategoriaDTO;
import com.fintrack.exception.ErroResponse;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categorias", description = "Categorias globais do sistema (imutáveis) e customizadas por usuário")
@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Listar categorias visíveis ao usuário",
            description = "Retorna as categorias globais do sistema mais as customizadas do próprio usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorias",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoriaDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} da rota não é o do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public List<CategoriaDTO> listar(@Parameter(description = "Id do usuário dono das categorias", example = "1")
                                      @PathVariable Long usuarioId,
                                      @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return categoriaService.listar(usuarioId);
    }

    @Operation(summary = "Criar categoria customizada",
            description = "Cria uma categoria pertencente ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada",
                    content = @Content(schema = @Schema(implementation = CategoriaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nome ausente ou em branco",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} da rota não é o do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe categoria com esse nome (global ou própria)",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CategoriaDTO> criar(@Parameter(description = "Id do usuário dono da categoria", example = "1")
                                               @PathVariable Long usuarioId,
                                               @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                               @Valid @RequestBody CategoriaDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        CategoriaDTO criada = categoriaService.criar(usuarioId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @Operation(summary = "Atualizar categoria customizada",
            description = "Só é permitido para categorias do próprio usuário — categoria global é imutável (403) "
                    + "e categoria de outro usuário retorna 404, igual a uma inexistente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada",
                    content = @Content(schema = @Schema(implementation = CategoriaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nome ausente ou em branco",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Categoria é global — não pode ser alterada",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, ou categoria inexistente/de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe categoria com esse nome (global ou própria)",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public CategoriaDTO atualizar(@Parameter(description = "Id do usuário dono da categoria", example = "1")
                                   @PathVariable Long usuarioId,
                                   @Parameter(description = "Id da categoria", example = "9")
                                   @PathVariable Long id,
                                   @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                   @Valid @RequestBody CategoriaDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return categoriaService.atualizar(usuarioId, id, dto);
    }

    @Operation(summary = "Excluir categoria customizada",
            description = "Categoria global não pode ser excluída (403); categoria com transações associadas também não (409).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria excluída"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Categoria é global — não pode ser excluída",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "{usuarioId} não confere, ou categoria inexistente/de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Categoria possui transações associadas",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do usuário dono da categoria", example = "1")
                                         @PathVariable Long usuarioId,
                                         @Parameter(description = "Id da categoria", example = "9")
                                         @PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        categoriaService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {usuarioId} da rota tem que ser o do token — se nao bater, 404
     * (nunca 403: confirmar que o id pertence a outra pessoa ja seria vazar
     * informacao).
     */
    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }
}
