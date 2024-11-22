-- Ruller tilbake vedtak som ble fattet, hvor resten av flyten i behandling feilet
UPDATE vedtak SET vedtakstatus = 'OPPRETTET', datofattet = null, fattetvedtakenhet = null WHERE id = 36500;