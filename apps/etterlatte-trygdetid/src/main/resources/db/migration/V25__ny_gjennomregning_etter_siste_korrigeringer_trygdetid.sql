alter table trygdetid_avvik add column status_v2 TEXT default 'IKKE_SJEKKET';
alter table trygdetid_avvik add column avvik_v2 JSONB default null;