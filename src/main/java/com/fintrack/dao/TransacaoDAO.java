package com.fintrack.dao;

import com.fintrack.model.Transacao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAO {

    private final Connection conexao;

    public TransacaoDAO() {
        this.conexao = Conexao.getConexao();
    }

    public TransacaoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public void inserir(Transacao t) {
        String sql = "INSERT INTO transacoes (descricao, valor, tipo, data) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getDescricao());
            ps.setDouble(2, t.getValor());
            ps.setString(3, t.getTipo());
            ps.setString(4, t.getData().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir transação", e);
        }
    }

    public List<Transacao> listarTodas() {
        String sql = "SELECT * FROM transacoes ORDER BY data DESC";
        List<Transacao> lista = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar transações", e);
        }
        return lista;
    }

    public List<Transacao> listarPorTipo(String tipo) {
        String sql = "SELECT * FROM transacoes WHERE tipo = ? ORDER BY data DESC";
        List<Transacao> lista = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar por tipo", e);
        }
        return lista;
    }

    public void atualizar(Transacao t) {
        String sql = "UPDATE transacoes SET descricao=?, valor=?, tipo=?, data=? WHERE id=?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, t.getDescricao());
            ps.setDouble(2, t.getValor());
            ps.setString(3, t.getTipo());
            ps.setString(4, t.getData().toString());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar transação", e);
        }
    }

    public void deletar(int id) {
        String sql = "DELETE FROM transacoes WHERE id=?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar transação", e);
        }
    }

    public double somarPorTipo(String tipo) {
        String sql = "SELECT COALESCE(SUM(valor), 0) FROM transacoes WHERE tipo=?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao somar por tipo", e);
        }
        return 0;
    }

    private Transacao mapear(ResultSet rs) throws SQLException {
        return new Transacao(
                rs.getInt("id"),
                rs.getString("descricao"),
                rs.getDouble("valor"),
                rs.getString("tipo"),
                LocalDate.parse(rs.getString("data"))
        );
    }
}