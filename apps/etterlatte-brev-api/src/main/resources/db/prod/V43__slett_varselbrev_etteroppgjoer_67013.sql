-- Sletting av foreldreløst barn
-- Forbehandlingen har fjernet brevreferansen etter beregning med resultat INGEN_ENDRING_UTEN_UTBETALING.
-- Automatisk sletting feilet fordi brevet er knyttet til en behandling.
INSERT INTO hendelse (brev_id, status_id, payload, opprettet, saksbehandler)
VALUES (67013, 'SLETTET', '{}', DEFAULT, NULL);
