-- Product Service — ensure pgvector extension is available
-- The vector_store table is managed by Spring AI (initialize-schema: true)
CREATE EXTENSION IF NOT EXISTS vector;
