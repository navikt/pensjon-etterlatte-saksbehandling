create TABLE vilkaarsvurdering (
    behandlingId UUID PRIMARY KEY,
    payload JSON,
    vilkaar JSONB,
    resultat JSONB
);
