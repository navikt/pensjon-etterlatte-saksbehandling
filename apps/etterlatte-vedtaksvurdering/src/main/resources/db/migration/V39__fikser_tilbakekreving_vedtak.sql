-- Tilbakestille status i vedtak om tilbakekreving som er låst
UPDATE vedtak SET vedtakstatus = 'OPPRETTET', attestertvedtakenhet = null, attestant = null, datoattestert = null WHERE id = 64597;
