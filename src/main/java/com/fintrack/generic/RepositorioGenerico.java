package com.fintrack.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RepositorioGenerico<T> {

    private final List<T> itens = new ArrayList<>();

    public void adicionar(T item) {
        itens.add(item);
    }

    public void remover(T item) {
        itens.remove(item);
    }

    public List<T> listarTodos() {
        return new ArrayList<>(itens);
    }

    public int tamanho() {
        return itens.size();
    }

    public void limpar() {
        itens.clear();
    }

    // ? extends T — aceita qualquer subtipo de T
    public void adicionarTodos(Collection<? extends T> colecao) {
        itens.addAll(colecao);
    }

    // ? super T — copia para destino que aceite T ou supertipo
    public void copiarPara(Collection<? super T> destino) {
        destino.addAll(itens);
    }

    // ? irrestrito — exibe qualquer repositório sem restrição de tipo
    public static void exibirTodos(RepositorioGenerico<?> repositorio) {
        for (Object item : repositorio.itens) {
            System.out.println(item);
        }
    }
}