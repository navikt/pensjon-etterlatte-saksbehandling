-- Ikke lenger i bruk
alter table innhold
    drop column mal;

-- Flytte tittel fra brev til innhold
alter table innhold
    add tittel text;

update innhold
set tittel = b.tittel
from brev b
where b.id = innhold.brev_id;

alter table brev
    drop column tittel;

-- Sette spraak NOT NULL
update innhold
set spraak = 'NB'
where spraak is null;

alter table innhold
    alter column spraak set not null;

-- Flytte PDF til egen tabell
create table pdf
(
    brev_id BIGINT NOT NULL
        PRIMARY KEY
        REFERENCES brev (id)
            ON DELETE CASCADE,
    bytes   BYTEA
);

insert into pdf (brev_id, bytes)
(select i.brev_id, i.bytes from innhold i where i.bytes is not null);

alter table innhold
    drop column bytes;

alter table pdf
    alter column bytes set not null;

-- Legge til sak_id
alter table brev
    add sak_id bigint not null default -1;
alter table brev
    alter column sak_id drop default;
