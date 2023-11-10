ALTER TABLE stoenad ADD COLUMN pesysid BIGINT;
ALTER TABLE stoenad  ADD COLUMN kilde text;

ALTER TABLE maaned_stoenad ADD COLUMN pesysid BIGINT;
ALTER TABLE maaned_stoenad   ADD COLUMN kilde text;
