-- fjerner saker med skjerming fra stønadsstatistikken
delete from maaned_stoenad where sakid in (11651,11652,12373,12402,12404,12472);
delete from stoenad where sakid in (11651,11652,12373,12402,12404,12472);

-- rekjør juli
delete from maaned_stoenad where statistikkmaaned = '2024-07';
delete from maanedsstatistikk_job where statistikkmaaned = '2024-07';
