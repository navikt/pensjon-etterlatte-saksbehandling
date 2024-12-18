-- Setter riktig status i vedtak om tilbakekreving
UPDATE vedtak SET vedtakstatus = 'FATTET_VEDTAK', attestertvedtakenhet = null, attestant = null, datoattestert = null WHERE id = 37057;
