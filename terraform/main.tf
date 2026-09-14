terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "f1rsters-tech-challenge-terraform-state"
    key            = "tech-challenge-mecanica/terraform-lambda.tfstate"
    region         = "sa-east-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      Component   = "lambda-auth"
    }
  }
}

# ---------------------------------------------------------------------------
# Lê o endpoint do RDS diretamente do state do repo terraform-bd, evitando
# ter que copiar/colar esse valor manualmente a cada deploy.
# ---------------------------------------------------------------------------
data "terraform_remote_state" "bd" {
  backend = "s3"
  config = {
    bucket = "f1rsters-tech-challenge-terraform-state"
    key    = "tech-challenge-mecanica/terraform-bd.tfstate"
    region = "sa-east-1"
  }
}

# S3 Bucket com os artefatos da Lambda (bucket já existente, criado manualmente)
data "aws_s3_bucket" "lambda_artifacts" {
  bucket = var.lambda_artifacts_bucket
}

# ---------------------------------------------------------------------------
# IAM Role da Lambda
# ---------------------------------------------------------------------------
resource "aws_iam_role" "lambda_auth" {
  name = "${var.project_name}-lambda-auth-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "${var.project_name}-lambda-auth-role"
  }
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_auth.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_rds_access" {
  name = "${var.project_name}-lambda-rds-access"
  role = aws_iam_role.lambda_auth.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "rds-db:connect"
        ]
        Resource = "*"
      }
    ]
  })
}

# ---------------------------------------------------------------------------
# Lambda Function de Autenticação
# ---------------------------------------------------------------------------
resource "aws_lambda_function" "auth" {
  function_name = "${var.project_name}-auth-function"
  role          = aws_iam_role.lambda_auth.arn
  runtime       = "java17"
  handler       = "com.f1rsters.tech_challenge_mecanica.lambda.AuthHandler::handleRequest"

  s3_bucket = data.aws_s3_bucket.lambda_artifacts.id
  s3_key    = var.lambda_auth_s3_key

  timeout     = 15
  memory_size = 256

  environment {
    variables = {
      DB_HOST     = data.terraform_remote_state.bd.outputs.rds_endpoint
      DB_NAME     = var.db_name
      DB_USERNAME = var.db_username
      DB_PASSWORD = var.db_password
      JWT_SECRET  = var.jwt_secret
    }
  }

  tags = {
    Name = "${var.project_name}-auth-function"
  }
}

# ---------------------------------------------------------------------------
# API Gateway
# ---------------------------------------------------------------------------
resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project_name}-api"
  protocol_type = "HTTP"

  tags = {
    Name = "${var.project_name}-api"
  }
}

resource "aws_apigatewayv2_stage" "dev" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = var.environment
  auto_deploy = true

  tags = {
    Name = "${var.project_name}-api-${var.environment}"
  }
}

resource "aws_apigatewayv2_integration" "auth_lambda" {
  api_id           = aws_apigatewayv2_api.main.id
  integration_type = "AWS_PROXY"

  integration_uri    = aws_lambda_function.auth.arn
  integration_method = "POST"

  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "auth" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /auth/login"

  target = "integrations/${aws_apigatewayv2_integration.auth_lambda.id}"
}

resource "aws_lambda_permission" "apigateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_apigatewayv2_api.main.execution_arn}/*/*"
}
