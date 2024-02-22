alter table brev
    drop constraint brev_behandling_id_key;

alter table brev
    add unique (behandling_id, brevtype);