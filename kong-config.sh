#!/bin/sh

# Wait for Kong to be ready
echo "Waiting for Kong to be ready..."
until curl -f http://one-gate-payment-gateway:8001/status; do
    echo "Kong is not ready yet. Waiting..."
    sleep 5
done

echo "Kong is ready! Configuring services and routes..."

# Create Authentication Service
curl -i -X POST http://one-gate-payment-gateway:8001/services/ \
  --data "name=authentication-service" \
  --data "url=http://authentication-one-gate-payment:8081"

# Create Payment Service
curl -i -X POST http://one-gate-payment-gateway:8001/services/ \
  --data "name=payment-service" \
  --data "url=http://payment-one-gate-payment:8082"

# Create routes for Authentication Service
curl -i -X POST http://one-gate-payment-gateway:8001/services/authentication-service/routes \
  --data "name=auth-register" \
  --data "paths[]=/api/auth/register" \
  --data "methods[]=POST" \
  --data "strip_path=false"

curl -i -X POST http://one-gate-payment-gateway:8001/services/authentication-service/routes \
  --data "name=auth-login" \
  --data "paths[]=/api/auth/login" \
  --data "methods[]=POST" \
  --data "strip_path=false"

curl -i -X POST http://one-gate-payment-gateway:8001/services/authentication-service/routes \
  --data "name=auth-refresh" \
  --data "paths[]=/api/auth/refresh" \
  --data "methods[]=PUT" \
  --data "strip_path=false"

curl -i -X POST http://one-gate-payment-gateway:8001/services/authentication-service/routes \
  --data "name=auth-logout" \
  --data "paths[]=/api/auth/logout" \
  --data "methods[]=DELETE" \
  --data "strip_path=false"

# Create routes for Payment Service
curl -i -X POST http://one-gate-payment-gateway:8001/services/payment-service/routes \
  --data "name=payment-account" \
  --data "paths[]=/api/payment/account" \
  --data "methods[]=GET" \
  --data "strip_path=false"

# Enable CORS plugin for all routes
echo "Enabling CORS for all routes..."
curl -i -X POST http://one-gate-payment-gateway:8001/plugins/ \
  --data "name=cors" \
  --data "config.origins=*" \
  --data "config.methods=GET,POST,PUT,DELETE,OPTIONS" \
  --data "config.headers=Accept,Accept-Version,Content-Length,Content-MD5,Content-Type,Date,X-Auth-Token,Authorization,X-API-Key" \
  --data "config.exposed_headers=X-Auth-Token" \
  --data "config.credentials=true" \
  --data "config.max_age=3600"

# Enable Rate Limiting
echo "Enabling rate limiting..."
curl -i -X POST http://one-gate-payment-gateway:8001/plugins/ \
  --data "name=rate-limiting" \
  --data "config.minute=100" \
  --data "config.hour=1000" \
  --data "config.policy=local"

echo "Configuring JWT authentication for protected routes..."

# Create a consumer for JWT
curl -i -X POST http://one-gate-payment-gateway:8001/consumers/ \
  --data "username=payment-app"

# Create JWT credentials for the consumer
curl -i -X POST http://one-gate-payment-gateway:8001/consumers/payment-app/jwt \
  --data "algorithm=HS512" \
  --data "key=https://bni.co.id" \
  --data "secret=e2926909a5c43ecea3ffa3b65fc747089755d687f401cd01e1a9372ab2d39e0700f108b7a6e739170e0cd25ef4f7bc9fdcc9b71de8d1be7e6e4490aa35d191a2"

# Enable JWT plugin for Payment Service (this runs first)
curl -i -X POST http://one-gate-payment-gateway:8001/services/payment-service/plugins \
  --data "name=jwt" \
  --data "config.secret_is_base64=false"

# Enable request-transformer plugin AFTER JWT (this runs second)
curl -i -X POST http://one-gate-payment-gateway:8001/services/payment-service/plugins \
  --data "name=request-transformer" \
  --data "config.add.headers[]=X-Username:\$(jwt.claims.sub)" \
  --data "ordering.after[]=jwt"

echo "Kong configuration completed!"
echo "Gateway is available at: http://localhost:8000"
echo "Admin API is available at: http://localhost:8001"
echo ""
echo "Test endpoints:"
echo "POST http://localhost:8000/api/auth/register"
echo "POST http://localhost:8000/api/auth/login"
echo "GET http://localhost:8000/api/payment/account (requires JWT token)"