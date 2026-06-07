package com.fintrack.controller;

import com.fintrack.model.Transacao;
import com.fintrack.service.FinanceiroService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;

public class MainController {

    @FXML private TableView<Transacao>              tabelaTransacoes;
    @FXML private TableColumn<Transacao, Integer>   colId;
    @FXML private TableColumn<Transacao, String>    colDescricao;
    @FXML private TableColumn<Transacao, Double>    colValor;
    @FXML private TableColumn<Transacao, String>    colTipo;
    @FXML private TableColumn<Transacao, LocalDate> colData;
    @FXML private Label lblReceitas;
    @FXML private Label lblDespesas;
    @FXML private Label lblSaldo;

    private final FinanceiroService service = new FinanceiroService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        carregarDados();
    }

    @FXML
    private void abrirFormulario() {
        abrirForm(null);
    }

    @FXML
    private void abrirFormularioAtualizar() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma transação para atualizar.").showAndWait();
            return;
        }
        abrirForm(selecionada);
    }

    private void abrirForm(Transacao transacao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/form.fxml"));
            Parent root = loader.load();
            FormController fc = loader.getController();
            fc.setTransacao(transacao);
            fc.setOnSalvar(this::carregarDados);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(transacao == null ? "Nova Transação" : "Editar Transação");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao abrir formulário", e);
        }
    }

    @FXML
    private void removerSelecionada() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma transação para remover.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmar remoção de \"" + selecionada.getDescricao() + "\"?");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                service.remover(selecionada.getId());
                carregarDados();
            }
        });
    }

    private void carregarDados() {
        tabelaTransacoes.setItems(FXCollections.observableArrayList(service.listarTodas()));
        atualizarTotais();
    }

    private void atualizarTotais() {
        lblReceitas.setText(String.format("R$ %.2f", service.calcularTotalReceitas()));
        lblDespesas.setText(String.format("R$ %.2f", service.calcularTotalDespesas()));
        lblSaldo.setText(String.format("R$ %.2f", service.calcularSaldo()));
    }
}