alter table utbetalingslinje add column kjoereplan VARCHAR(20);

update utbetalingslinje set kjoereplan = 'N';

alter table utbetalingslinje alter column kjoereplan SET NOT NULL;