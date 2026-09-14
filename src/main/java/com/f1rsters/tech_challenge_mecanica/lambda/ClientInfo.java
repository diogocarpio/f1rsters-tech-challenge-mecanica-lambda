package com.f1rsters.tech_challenge_mecanica.lambda;

public class ClientInfo {
    private Long id;
    private String nome;
    private String cpf;
    private String status;
    
    public ClientInfo(Long id, String nome, String cpf, String status) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.status = status;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public String getCpf() {
        return cpf;
    }
    
    public String getStatus() {
        return status;
    }
}
