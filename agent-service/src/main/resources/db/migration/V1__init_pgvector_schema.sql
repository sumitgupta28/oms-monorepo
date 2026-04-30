-- PgVector store for product semantic search
-- Matches the schema Spring AI PgVectorStore creates when initializeSchema=true
-- (HNSW index, cosine distance, 768-dim nomic-embed-text embeddings)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(768)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store
    USING HNSW (embedding vector_cosine_ops);
