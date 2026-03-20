#!/bin/sh
set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-wissensmanagement}"
DB_USERNAME="${DB_USERNAME:-portal}"

echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -q; do
  sleep 1
done
echo "PostgreSQL is ready."

# Create database if it does not exist
EXISTS=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'")
if [ "$EXISTS" != "1" ]; then
  echo "Creating database '$DB_NAME'..."
  PGPASSWORD="$DB_PASSWORD" createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" "$DB_NAME"
  echo "Database created."
else
  echo "Database '$DB_NAME' already exists."
fi

exec java -jar /app/app.jar
