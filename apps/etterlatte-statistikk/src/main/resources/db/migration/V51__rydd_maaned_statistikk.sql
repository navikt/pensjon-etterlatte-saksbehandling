-- fjerner saker med skjerming fra stønadsstatistikken
delete from stoenad where sakid in (select distinct sak_id from sak where ansvarlig_enhet = '2103');
delete from maaned_stoenad where sakid in (select distinct sak_id from sak where ansvarlig_enhet = '2103');

-- rekjør juli
delete from maaned_stoenad where statistikkmaaned = '2024-07';
delete from maanedsstatistikk_job where statistikkmaaned = '2024-07';
