package com.fintrack.model;

import java.time.LocalDate;

public class Transacao {

    private int id;
    private String descricao;
    private double valor;
    private String tipo;        // "RECEITA" ou "DESPESA"
    private LocalDate data;

    // Construtor vazio — exigido por frameworks javafx
    //
    // e útil para criar e preencher depois
    public Transacao() {
    }

    // Construtor sem id — usado ao criar uma transação nova (o id vem do banco)
    public Transacao(String descricao, double valor, String tipo, LocalDate data) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.data = data;
    }

    // Construtor completo — usado ao ler do banco (já tem id)
    public Transacao(int id, String descricao, double valor, String tipo, LocalDate data) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Transacao{id=" + id + ", descricao='" + descricao +
                "', valor=" + valor + ", tipo='" + tipo + "', data=" + data + '}';
    }
}