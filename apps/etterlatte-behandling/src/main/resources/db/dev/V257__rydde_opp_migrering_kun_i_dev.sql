-- denne kolonnen ble lagt til når en branch med migreringen gikk til dev
-- må ryddes bort i dev så migrering går igjennom der
alter table etteroppgjoer_behandling drop column brev_id;
