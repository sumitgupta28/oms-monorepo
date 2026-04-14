-- postgres/init.sql
-- Creates all service databases on first Docker Compose run

CREATE DATABASE keycloak;
CREATE DATABASE orders;
CREATE DATABASE payments;
CREATE DATABASE inventory;
CREATE DATABASE products;   -- used by product-service for pgvector

-- Enable pgvector extension in products DB
\c products
CREATE EXTENSION IF NOT EXISTS vector;
