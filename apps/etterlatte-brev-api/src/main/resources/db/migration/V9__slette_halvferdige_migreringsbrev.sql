BEGIN TRANSACTION;
DELETE FROM hendelse WHERE brev_id in (298, 10991, 11529, 11535) and status_id='OPPRETTET';
DELETE FROM innhold WHERE brev_id in (298, 10991, 11529, 11535);
DELETE FROM mottaker WHERE brev_id in (298, 10991, 11529, 11535);
DELETE FROM pdf WHERE brev_id in (298, 10991, 11529, 11535);
DELETE FROM brev WHERE id in (298, 10991, 11529, 11535);
COMMIT TRANSACTION;