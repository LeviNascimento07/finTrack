package com.fintrack.service;

import com.fintrack.dao.TransacaoDAO;
import com.fintrack.model.Transacao;

import java.util.List;

public class FinanceiroService {

    private final TransacaoDAO dao;

    public FinanceiroService() {
        this.dao = new TransacaoDAO();
    }

    public FinanceiroService(TransacaoDAO dao) {
        this.dao = dao;
    }

    public void salvar(Transacao t) {
        validar(t);
        dao.inserir(t);
    }

    public void atualizar(Transacao t) {
        validar(t);
        dao.atualizar(t);
    }

    public void remover(int id) {
        dao.deletar(id);
    }

    public List<Transacao> listarTodas() {
        return dao.listarTodas();
    }

    public double calcularTotalReceitas() {
        return dao.somarPorTipo("RECEITA");
    }

    public double calcularTotalDespesas() {
        return dao.somarPorTipo("DESPESA");
    }

    public double calcularSaldo() {
        return calcularTotalReceitas() - calcularTotalDespesas();
    }

    public void validar(Transacao t) {
        if (t == null)
            throw new IllegalArgumentException("Transação não pode ser nula");
        if (t.getDescricao() == null || t.getDescricao().isBlank())
            throw new IllegalArgumentException("Descrição é obrigatória");
        if (t.getValor() <= 0)
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        if (t.getTipo() == null || (!t.getTipo().equals("RECEITA") && !t.getTipo().equals("DESPESA")))
            throw new IllegalArgumentException("Tipo deve ser RECEITA ou DESPESA");
        if (t.getData() == null)
            throw new IllegalArgumentException("Data é obrigatória");
    }
}