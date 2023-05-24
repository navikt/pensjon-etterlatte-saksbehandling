alter table utbetalingslinje add column bruk_kjoereplan VARCHAR(20);

update utbetalingslinje set bruk_kjoereplan = 'N';

alter table utbetalingslinje alter column bruk_kjoereplan SET NOT NULL;