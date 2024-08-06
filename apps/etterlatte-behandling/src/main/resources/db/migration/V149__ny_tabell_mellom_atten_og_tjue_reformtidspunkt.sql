-- Oppretter tabell hvor det vil importeres uttrekk fra de som var mellom 18 og 20 år på
-- reformtidspunkt og hvor de var over 18 år når en av foreldrene døde
CREATE TABLE mellom_atten_og_tjue_ved_reformtidspunkt
(
    id BIGSERIAL
        PRIMARY KEY,
    fnr TEXT NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    endret TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    feilmelding TEXT
);

-- Legger til kolonne i doedshendelse som markerer at et innslag har oppstått pga migreringsjobb
-- for de mellom 18 og 20 år på reformtidspunkt
ALTER TABLE doedshendelse ADD COLUMN migrert_mellom_atten_og_tjue BOOLEAN DEFAULT false;
UPDATE doedshendelse SET migrert_mellom_atten_og_tjue = false;
ALTER TABLE doedshendelse ALTER COLUMN migrert_mellom_atten_og_tjue SET NOT NULL;