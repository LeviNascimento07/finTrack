package com.fintrack.controller;

import com.fintrack.model.Transacao;
import com.fintrack.service.FinanceiroService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class FormController {

    @FXML private TextField        txtDescricao;
    @FXML private TextField        txtValor;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private DatePicker       dtpData;
    @FXML private Label            lblErro;

    private final FinanceiroService service = new FinanceiroService();
    private Transacao transacao;
    private Runnable onSalvar;

    @FXML
    public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("RECEITA", "DESPESA"));
    }

    public void setTransacao(Transacao t) {
        this.transacao = t;
        if (t != null) {
            txtDescricao.setText(t.getDescricao());
            txtValor.setText(String.valueOf(t.getValor()));
            cmbTipo.setValue(t.getTipo());
            dtpData.setValue(t.getData());
        }
    }

    public void setOnSalvar(Runnable callback) {
        this.onSalvar = callback;
    }

    @FXML
    private void salvar() {
        lblErro.setText("");
        try {
            String descricao = txtDescricao.getText();
            double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
            String tipo = cmbTipo.getValue();
            LocalDate data = dtpData.getValue();

            if (transacao == null) {
                service.salvar(new Transacao(descricao, valor, tipo, data));
            } else {
                transacao.setDescricao(descricao);
                transacao.setValor(valor);
                transacao.setTipo(tipo);
                transacao.setData(data);
                service.atualizar(transacao);
            }

            if (onSalvar != null) onSalvar.run();
            fechar();
        } catch (NumberFormatException e) {
            lblErro.setText("Valor inválido. Use ponto como separador decimal.");
        } catch (IllegalArgumentException e) {
            lblErro.setText(e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        fechar();
    }

    private void fechar() {
        ((Stage) txtDescricao.getScene().getWindow()).close();
    }
}