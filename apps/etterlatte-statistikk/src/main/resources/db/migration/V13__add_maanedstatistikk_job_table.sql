CREATE TABLE maanedsstatistikk_job
(
    id BIGSERIAL PRIMARY KEY,
    statistikkMaaned TEXT,
    kjoertStatus TEXT,
    raderRegistrert BIGINT,
    raderMedFeil BIGINT
);