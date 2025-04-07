CREATE TABLE etteroppgjoer_faktisk_inntekt(
    id UUID PRIMARY KEY,
    forbehandling_id UUID,
    loennsinntekt BIGINT NOT NULL,
    afp BIGINT NOT NULL,
    naeringsinntekt BIGINT NOT NULL,
    utland BIGINT NOT NULL,
    spesifikasjon_av_inntekt TEXT NOT NULL,
    opprettet TIMESTAMP DEFAULT NOW()
)