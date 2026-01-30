CREATE TABLE doedshendelse_ukjente_beroerte(
    avdoed_fnr TEXT PRIMARY KEY,
    barn_uten_ident JSONB,
    ektefeller_uten_ident JSONB
)