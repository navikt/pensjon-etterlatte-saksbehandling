ALTER TABLE pesyskopling ALTER COLUMN pesys_id TYPE BIGINT USING (pesys_id::bigint)