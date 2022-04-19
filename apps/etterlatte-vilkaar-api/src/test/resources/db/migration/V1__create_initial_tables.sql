CREATE TABLE vurdertvilkaar
(
    behandling UUID,
    avdoedSoeknad TEXT,
    soekerSoeknad TEXT,
    soekerPdl TEXT,
    avdoedPdl TEXT,
    gjenlevendePdl TEXT,
    versjon BIGINT,
    vilkaarResultat TEXT,
    PRIMARY KEY(behandling, versjon)
);