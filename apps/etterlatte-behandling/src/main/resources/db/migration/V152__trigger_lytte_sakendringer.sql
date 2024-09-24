-- Basert på https://www.cybertec-postgresql.com/en/tracking-changes-in-postgresql/
CREATE SCHEMA logging;

CREATE TABLE logging.t_history
(
    id          serial,
    timestamp   timestamp DEFAULT now(),
    skjema      text,
    tabell      text,
    operation   text,
    utfoerende  text      DEFAULT current_user,
    foer        jsonb,
    etter       jsonb
);

CREATE FUNCTION logging.change_trigger() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'INSERT'
    THEN
        INSERT INTO logging.t_history (tabell, skjema, operation, foer)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP, row_to_json(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE'
    THEN
        INSERT INTO logging.t_history (tabell, skjema, operation, foer, etter)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP,
                row_to_json(NEW), row_to_json(OLD));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE'
    THEN
        INSERT INTO logging.t_history (tabell, skjema, operation, etter)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP, row_to_json(OLD));
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE 'plpgsql' SECURITY DEFINER;

CREATE TRIGGER audit_sak
    AFTER INSERT OR UPDATE OR DELETE
    ON sak
    FOR EACH ROW
EXECUTE PROCEDURE logging.change_trigger();

CREATE TRIGGER audit_behandling
    AFTER INSERT OR UPDATE OR DELETE
    ON behandling
    FOR EACH ROW
EXECUTE PROCEDURE logging.change_trigger();