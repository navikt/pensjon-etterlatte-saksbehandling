CREATE TABLE ukjente_beroerte_av_doedshendelse(
    avdoed_fnr TEXT PRIMARY KEY,
    barn_uten_ident JSONB,
    ektefeller_uten_ident JSONB
)