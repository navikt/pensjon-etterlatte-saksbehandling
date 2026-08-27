-- Legger til metadata-kolonnen (jsonb) som biblioteket (efterlatte-prosessering
-- v1.20260824100828_0d24e53) nå leser/skriver via Task.metadata / insert(...).
-- Verifisert mot den faktiske publiserte PostgresTaskRepository (sources jar):
-- kolonnen deserialiseres med Metadata.deserialiser(getString("metadata")) og
-- insertSql skriver ?::jsonb inn i "metadata".
ALTER TABLE public.prosessering_task
    ADD COLUMN IF NOT EXISTS metadata JSONB;
