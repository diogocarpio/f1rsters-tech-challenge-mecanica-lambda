variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "sa-east-1"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "f1rsters-tech-challenge-mecanica"
}

variable "environment" {
  description = "Environment name (dev, homolog, prod) - tambem usado como nome do stage do API Gateway"
  type        = string
  default     = "dev"
}

variable "lambda_artifacts_bucket" {
  description = "S3 bucket com os artefatos (JARs) da Lambda"
  type        = string
  default     = "f1rsters-tech-challenge-lambda-artifacts"
}

variable "lambda_auth_s3_key" {
  description = "Chave (path) do JAR da Lambda de autenticacao no S3"
  type        = string
  default     = "auth-function.jar"
}

variable "db_name" {
  description = "Nome do banco de dados (deve ser o mesmo definido no repo terraform-bd)"
  type        = string
  default     = "oficina"
}

variable "db_username" {
  description = "Usuario do banco de dados (deve ser o mesmo definido no repo terraform-bd)"
  type        = string
  default     = "oficinauser"
}

variable "db_password" {
  description = "Senha do banco de dados (deve ser a mesma definida no repo terraform-bd)"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Chave secreta JWT (Base64) usada para gerar/validar tokens"
  type        = string
  sensitive   = true
}
