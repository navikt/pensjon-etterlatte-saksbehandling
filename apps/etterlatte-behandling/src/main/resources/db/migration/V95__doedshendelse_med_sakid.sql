alter table doedshendelse ADD COLUMN sak_id BIGINT DEFAULT NULL;
update doedshendelse set sak_id = NULL;