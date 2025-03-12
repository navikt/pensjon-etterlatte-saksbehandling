CREATE TABLE pensjonsgivendeinntekt_fra_skatt (
    id UUID PRIMARY KEY,
    forbehandling_id UUID,
    inntektsaar INT NOT NULL,
    skatteordning TEXT NOT NULL,
    loensinntekt BIGINT NOT NULL,
    naeringsinntekt BIGINT NOT NULL,
    fiske_fangst_familiebarnehage BIGINT NOT NULL,
    opprettet TIMESTAMP DEFAULT NOW()
);
