CREATE TABLE skatteoppgjoer_hendelse_kjoringer (
    siste_sekvensnummer INT,
    antall_hendelser INT,
    antall_behandlet INT,
    antall_relevante INT,
    opprettet TIMESTAMP DEFAULT NOW()
);
