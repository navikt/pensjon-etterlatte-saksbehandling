create table pensjonsgivendeinntekt_fra_skatt
(
    id UUID PRIMARY KEY,
    forbehandling_id UUID,
    inntektsaar INT,
    skatteordning TEXT,
    loensinntekt BIGINT,
    naeringsinntekt BIGINT,
    fiske_fangst_familiebarnehage BIGINT,
    opprettet TIMESTAMP SET DEFAULT NOW(),
)