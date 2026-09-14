package com.f1rsters.tech_challenge_mecanica.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String STATUS_CODE = "statusCode";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CpfValidator cpfValidator;
    private final JwtService jwtService;
    private final DatabaseService databaseService;

    public AuthHandler() {
        this.cpfValidator = new CpfValidator();
        this.jwtService = JwtService.create();
        this.databaseService = new DatabaseService();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            context.getLogger().log("Processing authentication request");
            
            // Extract CPF from request
            String cpf = extractCpf(input);
            context.getLogger().log("CPF received: " + maskCpf(cpf));
            
            // Validate CPF
            if (!cpfValidator.isValid(cpf)) {
                context.getLogger().log("Invalid CPF format");
                return buildErrorResponse(400, "CPF inválido");
            }
            
            // Check if client exists in database
            ClientInfo client = databaseService.getClientByCpf(cpf);
            if (client == null) {
                context.getLogger().log("Client not found");
                return buildErrorResponse(404, "Cliente não encontrado");
            }
            
            // Check client status
            if (!isClientStatusAllowed(client.getStatus())) {
                context.getLogger().log("Client status not allowed: " + client.getStatus());
                return buildErrorResponse(403, "Cliente com status não permitido");
            }
            
            // Generate JWT token
            String token = jwtService.generateToken(client);
            context.getLogger().log("JWT token generated successfully");
            
            // Build success response
            response.put(STATUS_CODE, 200);
            response.put("body", buildSuccessBody(client, token));
            
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            return buildErrorResponse(500, "Erro interno no servidor");
        }
        
        return response;
    }

    private String extractCpf(Map<String, Object> input) {
        if (input.containsKey("body")) {
            String body = (String) input.get("body");

            try {
                Map<String, String> bodyMap = objectMapper.readValue(body, Map.class);
                return bodyMap.getOrDefault("cpf", "");
            } catch (JsonProcessingException e) {
                return "";
            }
        }

        return input.getOrDefault("cpf", "").toString();
    }
    
    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return "***";
        return "***." + cpf.substring(3, 6) + ".***-" + cpf.substring(9);
    }
    
    private boolean isClientStatusAllowed(String status) {
        return "ATIVO".equalsIgnoreCase(status) || 
               "APROVADO".equalsIgnoreCase(status) ||
               "VERIFICADO".equalsIgnoreCase(status);
    }
    
    private Map<String, Object> buildSuccessBody(ClientInfo client, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", token);
        body.put("tokenType", "Bearer");
        body.put("expiresInSeconds", 900);
        body.put("client", Map.of(
            "id", client.getId(),
            "nome", client.getNome(),
            "cpfMascarado", maskCpf(client.getCpf())
        ));
        return body;
    }
    
    private Map<String, Object> buildErrorResponse(int statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put(STATUS_CODE, statusCode);
        response.put("body", Map.of(
            "error", message,
            STATUS_CODE, statusCode
        ));
        return response;
    }
}
