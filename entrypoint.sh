#!/bin/sh

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-wissensmanagement}"
DB_USERNAME="${DB_USERNAME:-portal}"

echo "=== Wissensmanagement Entrypoint ==="
echo "DB_HOST=${DB_HOST}, DB_PORT=${DB_PORT}, DB_NAME=${DB_NAME}"

# Wait for PostgreSQL with timeout
RETRIES=30
echo "Waiting for PostgreSQL..."
while ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -q 2>/dev/null; do
  RETRIES=$((RETRIES - 1))
  if [ "$RETRIES" -le 0 ]; then
    echo "WARNING: PostgreSQL not ready after 30s, starting app anyway"
    break
  fi
  sleep 1
done

if [ "$RETRIES" -gt 0 ]; then
  echo "PostgreSQL is ready"
  # Try to create database if it doesn't exist
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres \
    -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" 2>/dev/null | grep -q 1
  if [ $? -ne 0 ]; then
    echo "Creating database '${DB_NAME}'..."
    PGPASSWORD="$DB_PASSWORD" createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" "$DB_NAME" 2>&1 || echo "WARNING: createdb failed (may already exist)"
  else
    echo "Database '${DB_NAME}' exists"
  fi
fi

echo "Starting Spring Boot application..."
exec java -jar /app/app.jar
