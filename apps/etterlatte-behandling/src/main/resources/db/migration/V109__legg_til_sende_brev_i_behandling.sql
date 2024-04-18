alter table behandling ADD COLUMN sende_brev bool not null default true;

update behandling set sende_brev=false where revurdering_aarsak in (
 'ALDERSOVERGANG', 'DOEDSFALL', 'OPPHOER_UTEN_BREV', 'REGULERING', 'ANNEN_UTEN_BREV'
)
