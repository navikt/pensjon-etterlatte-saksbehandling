update sak set teknisk_tid = teknisk_tid + interval '1 second' where resultat_begrunnelse = 'BEHANDLING_RULLET_TILBAKE';
drop table tilbakestilte_behandlinger;
