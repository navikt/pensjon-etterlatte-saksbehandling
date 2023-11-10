create table bosattutland (
    behandlingid UUID PRIMARY KEY,
    rinanummer text,
    mottattSeder jsonb,
    sendteSeder jsonb
)