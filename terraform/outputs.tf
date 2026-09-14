output "lambda_function_arn" {
  description = "ARN da funcao Lambda de autenticacao"
  value       = aws_lambda_function.auth.arn
}

output "lambda_function_name" {
  description = "Nome da funcao Lambda de autenticacao"
  value       = aws_lambda_function.auth.function_name
}

output "api_gateway_endpoint" {
  description = "Endpoint do API Gateway (ex: para POST /auth/login)"
  value       = aws_apigatewayv2_stage.dev.invoke_url
}

output "api_id" {
  description = "ID do API Gateway"
  value       = aws_apigatewayv2_api.main.id
}
