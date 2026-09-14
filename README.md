# Tech Challenge FIAP - Lambda de Autenticação

## Propósito

Função **AWS Lambda** responsável pela autenticação de clientes via CPF e geração de tokens JWT, exposta publicamente através de um **API Gateway HTTP**. É o ponto de entrada de autenticação do sistema de gestão da oficina mecânica (Tech Challenge FIAP), mantido em repositório próprio para isolar seu ciclo de vida (build, testes, infraestrutura e deploy) do restante do sistema.

## Funcionalidades

- Validação de CPF (algoritmo oficial com dígitos verificadores)
- Consulta de cliente no banco de dados PostgreSQL (provisionado no repo `terraform-bd`)
- Verificação de status do cliente
- Geração de token JWT para clientes autorizados
- Tratamento de erros (CPF inválido, cliente inexistente, status não permitido)
- Mascaramento de dados sensíveis em logs e respostas

## Tecnologias utilizadas

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem de desenvolvimento |
| AWS Lambda | Plataforma serverless (runtime `java17`) |
| AWS API Gateway (HTTP API) | Exposição pública do endpoint de login |
| AWS SDK for Java 2.x | Integração com serviços AWS |
| PostgreSQL JDBC | Conexão com o banco de dados |
| JWT (jjwt) | Geração e validação de tokens |
| JUnit 5 / Mockito | Testes |
| Checkstyle / SpotBugs | Qualidade de código |
| Terraform >= 1.0 | Infraestrutura (Lambda, IAM, API Gateway) |
| GitHub Actions | CI/CD |

> **Sobre o Dockerfile:** este componente não usa container — o deploy é feito via **JAR** enviado ao S3 e atualizado diretamente na função Lambda (`aws lambda update-function-code`), que é o modelo padrão de deploy para Lambda em Java. Por isso não há `Dockerfile` neste repositório.

## Diagrama de arquitetura

```
┌──────────────┐      POST /auth/login      ┌───────────────────────┐
│   Cliente     │ ─────────────────────────► │   API Gateway (HTTP)  │
└──────────────┘                             └───────────┬───────────┘
                                                          │ AWS_PROXY
                                                          ▼
                                              ┌───────────────────────┐
                                              │  Lambda: AuthHandler   │
                                              │  (Java 17)             │
                                              │  ┌──────────────────┐ │
                                              │  │ CpfValidator      │ │
                                              │  │ DatabaseService   │ │
                                              │  │ JwtService         │ │
                                              │  └──────────────────┘ │
                                              └───────────┬───────────┘
                                                          │ JDBC
                                                          ▼
                                       ┌──────────────────────────────┐
                                       │  RDS PostgreSQL                │
                                       │  (repo terraform-bd)           │
                                       └──────────────────────────────┘
```

## Estrutura de pastas

```
mecanica-lambda/
├── src/
│   ├── main/java/com/f1rsters/tech_challenge_mecanica/lambda/
│   │   ├── AuthHandler.java      # Handler principal da Lambda
│   │   ├── CpfValidator.java     # Validador de CPF
│   │   ├── JwtService.java       # Geração/validação de JWT
│   │   ├── DatabaseService.java  # Acesso ao banco de dados
│   │   └── ClientInfo.java       # DTO de informações do cliente
│   └── test/java/.../CpfValidatorTest.java
├── terraform/
│   ├── main.tf           # IAM Role, Lambda Function, API Gateway
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars.{example,dev,homolog,prod}
├── .github/workflows/ci-cd.yml
├── pom.xml
└── README.md
```

## Pré-requisitos

- Java JDK 17
- Maven 3.8+
- AWS CLI configurado
- Terraform >= 1.0
- O repositório `terraform-bd` já aplicado (o Terraform aqui lê o endpoint do RDS via `terraform_remote_state`)
- Bucket S3 `f1rsters-tech-challenge-lambda-artifacts` já existente

## Passos para execução e deploy

### 1. Build e testes locais

```bash
mvn clean package
mvn test
```

O JAR é gerado em `target/auth-function.jar`.

### 2. Provisionar a infraestrutura (IAM + Lambda + API Gateway)

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edite terraform.tfvars com os valores da sua conta AWS

terraform init \
  -backend-config="bucket=f1rsters-tech-challenge-terraform-state" \
  -backend-config="key=tech-challenge-mecanica/terraform-lambda.tfstate" \
  -backend-config="region=sa-east-1" \
  -backend-config="encrypt=true"

terraform plan
terraform apply
```

### 3. Publicar o código atualizado na função já criada

```bash
aws s3 cp target/auth-function.jar s3://f1rsters-tech-challenge-lambda-artifacts/auth-function.jar

aws lambda update-function-code \
  --function-name f1rsters-tech-challenge-mecanica-auth-function \
  --s3-bucket f1rsters-tech-challenge-lambda-artifacts \
  --s3-key auth-function.jar
```

### 4. Testar o endpoint

```bash
curl -X POST https://<api-gateway-endpoint>/dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678909"}'
```

### Via CI/CD (GitHub Actions)

O pipeline (`.github/workflows/ci-cd.yml`) executa, em sequência:

1. **Build e testes** (Maven, Checkstyle, SpotBugs) em qualquer push/PR
2. **Terraform Plan** (comenta no PR)
3. **Terraform Apply** — apenas em `main`/`homologacao`
4. **Deploy da Lambda** — em `main`, `homologacao` ou `develop`

**Secrets necessários no repositório:**

| Secret | Descrição |
|---|---|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Credenciais AWS |
| `DB_PASSWORD` | Senha do banco (deve ser igual à usada no repo `terraform-bd`) |
| `JWT_SECRET` | Chave secreta JWT Base64 |

## Variáveis de ambiente da função (definidas via Terraform)

| Variável | Descrição |
|---|---|
| `DB_HOST` | Endpoint do RDS (lido automaticamente do repo `terraform-bd`) |
| `DB_NAME` | Nome do banco |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo para assinatura do JWT |

## Link do Swagger/Postman

A documentação Swagger completa da API vive no repositório da aplicação principal. Este endpoint (`POST /auth/login`) é a porta de entrada de autenticação usada por ela:

- Swagger/Postman: ver [f1rsters-tech-challenge-mecanica](https://github.com/diogocarpio/f1rsters-tech-challenge-mecanica#swagger--openapi)

## Testes

```bash
mvn test
```

## Repositórios relacionados

- [f1rsters-tech-challenge-mecanica](https://github.com/diogocarpio/f1rsters-tech-challenge-mecanica) — aplicação principal
- [f1rsters-tech-challenge-mecanica-terraform-bd](https://github.com/diogocarpio/f1rsters-tech-challenge-mecanica-terraform-bd) — banco de dados (fonte do `DB_HOST`)
- [f1rsters-tech-challenge-mecanica-terraform-kubernets](https://github.com/diogocarpio/f1rsters-tech-challenge-mecanica-terraform-kubernets) — cluster Kubernetes e observabilidade

## Grupo

**Turma:** F1RSTERS FIAP - Diogo, Alexandra, Rodrigo e Livea
