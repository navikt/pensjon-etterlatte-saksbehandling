CREATE TABLE overstyr_beregning(
   sak_id BIGINT PRIMARY KEY,
   beskrivelse TEXT NOT NULL,
   tidspunkt TIMESTAMP default NOW()
);