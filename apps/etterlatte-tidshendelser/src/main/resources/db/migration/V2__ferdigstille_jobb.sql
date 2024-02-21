CREATE OR REPLACE FUNCTION ferdigstillJobb() RETURNS TRIGGER AS
$$
BEGIN
    UPDATE jobb
    SET status  = 'FERDIG',
        endret  = CURRENT_TIMESTAMP,
        versjon = versjon + 1
    WHERE id = NEW.jobb_id
      AND NOT EXISTS (SELECT 1
                      FROM hendelse h
                      WHERE h.jobb_id = NEW.jobb_id
                        AND h.status <> 'FERDIG');
    RETURN NEW;
END ;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trig_jobb_ferdig
    AFTER UPDATE
    ON hendelse
    FOR EACH ROW
EXECUTE FUNCTION ferdigstillJobb();
