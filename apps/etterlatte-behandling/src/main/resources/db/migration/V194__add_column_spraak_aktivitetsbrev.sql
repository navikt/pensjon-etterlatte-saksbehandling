alter table aktivitetsplikt_brevdata add column spraak text;
update aktivitetsplikt_brevdata set spraak = 'NB' where spraak is null and brev_id is not null;
