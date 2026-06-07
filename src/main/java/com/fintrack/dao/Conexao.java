package com.fintrack.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

    private static final String URL = "jdbc:sqlite:fintrack.db";
    private static final String URL_TESTE = "jdbc:sqlite::memory:";

    public static Connection getConexao() {
        try {
            Connection conn = DriverManager.getConnection(URL);
            inicializarBanco(conn);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados", e);
        }
    }

    public static Connection getConexaoTeste() {
        try {
            Connection conn = DriverManager.getConnection(URL_TESTE);
            inicializarBanco(conn);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de testes", e);
        }
    }

    private static void inicializarBanco(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS transacoes (" +
                     "id        INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "descricao TEXT    NOT NULL, " +
                     "valor     REAL    NOT NULL, " +
                     "tipo      TEXT    NOT NULL, " +
                     "data      TEXT    NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}