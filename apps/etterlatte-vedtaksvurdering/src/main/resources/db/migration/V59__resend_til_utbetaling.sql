CREATE TABLE resend_til_utbetaling
(
    behandling_id UUID                      NOT NULL PRIMARY KEY,
    opprettet     TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    prosessert    BOOLEAN                   NOT NULL DEFAULT FALSE
);
