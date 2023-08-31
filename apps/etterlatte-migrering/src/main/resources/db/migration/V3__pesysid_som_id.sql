DROP TABLE pesyssak;
CREATE TABLE pesyssak
(
    id                  BIGINT PRIMARY KEY,
    opprettet           TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    sak                 JSONB NOT NULL,
    status              VARCHAR not null
);