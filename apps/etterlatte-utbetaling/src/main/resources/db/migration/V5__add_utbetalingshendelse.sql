TRUNCATE TABLE utbetaling, utbetalingslinje, avstemming;

ALTER TABLE utbetaling
    DROP COLUMN status;

CREATE TABLE utbetalingshendelse
(
    id            UUID PRIMARY KEY,
    utbetaling_id UUID references utbetaling (id),
    tidspunkt     TIMESTAMP WITH TIME ZONE NOT NULL,
    status        VARCHAR                  NOT NULL
)