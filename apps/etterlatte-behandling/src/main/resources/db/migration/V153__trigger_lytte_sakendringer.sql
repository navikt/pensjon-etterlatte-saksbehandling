-- Basert på https://www.cybertec-postgresql.com/en/tracking-changes-in-postgresql/
CREATE SCHEMA logging;

CREATE TABLE logging.t_history
(
    id         serial,
    timestamp  timestamp DEFAULT now(),
    schemaname text,
    tabname    text,
    operation  text,
    who        text,
    new_val    jsonb,
    old_val    jsonb
);

CREATE FUNCTION logging.change_trigger() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'INSERT'
    THEN
        INSERT INTO logging.t_history (tabname, schemaname, operation, new_val, who)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP, row_to_json(NEW), NEW.sistEndretAv);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE'
    THEN
        INSERT INTO logging.t_history (tabname, schemaname, operation, new_val, old_val, who)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP,
                row_to_json(NEW), row_to_json(OLD), NEW.sistEndretAv);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE'
    THEN
        -- TODO: who her er ikkje heilt opplagt
        INSERT INTO logging.t_history (tabname, schemaname, operation, old_val, who)
        VALUES (TG_RELNAME, TG_TABLE_SCHEMA, TG_OP, row_to_json(OLD), current_user);
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE 'plpgsql' SECURITY DEFINER;

CREATE TRIGGER audit_sak
    AFTER INSERT OR UPDATE OR DELETE
    ON sak
    FOR EACH ROW
EXECUTE PROCEDURE logging.change_trigger();