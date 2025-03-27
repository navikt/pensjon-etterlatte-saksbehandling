-- Setter riktig status i vedtak om tilbakekreving hvor det feilet med broken pipe
UPDATE vedtak SET vedtakstatus = 'OPPRETTET', fattetvedtakenhet = null, saksbehandlerid = null, datofattet = null WHERE id = 39930;
