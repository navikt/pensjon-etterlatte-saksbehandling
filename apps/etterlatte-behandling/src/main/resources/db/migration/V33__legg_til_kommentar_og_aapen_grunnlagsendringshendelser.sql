ALTER TABLE grunnlagsendringshendelse DROP COLUMN beskrivelse;
ALTER TABLE grunnlagsendringshendelse ADD COLUMN kommentar TEXT;
ALTER TABLE grunnlagsendringshendelse ADD COLUMN aapen BOOLEAN DEFAULT TRUE;