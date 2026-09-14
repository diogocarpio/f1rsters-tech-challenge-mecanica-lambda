package com.f1rsters.tech_challenge_mecanica.lambda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseService {
    
    private static final String DB_HOST = System.getenv("DB_HOST");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USERNAME = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    
    public ClientInfo getClientByCpf(String cpf) {
        String url = buildJdbcUrl();
        
        try (Connection connection = DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD)) {
            String query = "SELECT id, nome, cpf_cnpj, status FROM cliente WHERE cpf_cnpj = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, cpf);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new ClientInfo(
                            resultSet.getLong("id"),
                            resultSet.getString("nome"),
                            resultSet.getString("cpf_cnpj"),
                            resultSet.getString("status")
                        );
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Database error: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    private String buildJdbcUrl() {
        if (DB_HOST == null || DB_HOST.isEmpty()) {
            throw new IllegalStateException("DB_HOST environment variable not set");
        }
        
        // Extract host and port from RDS endpoint (format: hostname:port)
        String[] parts = DB_HOST.split(":");
        String host = parts[0];
        String port = parts.length > 1 ? parts[1] : "5432";
        
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, DB_NAME);
    }
}
