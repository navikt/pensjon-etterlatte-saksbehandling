delete from maaned_stoenad where statistikkmaaned in (select statistikkmaaned from maanedsstatistikk_job where kjoertstatus = 'FEIL');

delete from maanedsstatistikk_job where kjoertstatus = 'FEIL';

