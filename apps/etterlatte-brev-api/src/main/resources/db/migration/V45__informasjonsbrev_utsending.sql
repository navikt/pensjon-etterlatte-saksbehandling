CREATE TABLE informasjonsbrev_utsending (
    utsendings_id UUID PRIMARY KEY,
    sak_id BIGINT NOT NULL,
    innsender TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    request JSONB NOT NULL,
    brev_id BIGINT NOT NULL UNIQUE REFERENCES brev(id),
    status TEXT NOT NULL CHECK (status IN (
        'OPPRETTET', 'PDF_LAGRET', 'FERDIGSTILT', 'JOURNALFOERING_PAABEGYNT',
        'JOURNALFOERT', 'DISTRIBUSJON_PAABEGYNT', 'FERDIG', 'AVVENTER_AVKLARING', 'FEILA'
    )),
    eier UUID,
    laast_til TIMESTAMPTZ,
    feilkode TEXT,
    opprettet TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The guard is inside the write transaction, including existing mutation paths.
CREATE FUNCTION beskytt_batchbrev() RETURNS trigger AS $$
DECLARE
    brev_id_verdi BIGINT;
    utsending informasjonsbrev_utsending;
    batch_eier TEXT;
BEGIN
    IF TG_TABLE_NAME = 'brev' THEN
        brev_id_verdi := OLD.id;
    ELSE
        IF TG_OP = 'INSERT' THEN
            brev_id_verdi := NEW.brev_id;
        ELSE
            brev_id_verdi := OLD.brev_id;
        END IF;
    END IF;
    PERFORM 1 FROM brev WHERE id = brev_id_verdi FOR UPDATE;
    SELECT * INTO utsending FROM informasjonsbrev_utsending WHERE brev_id = brev_id_verdi;
    IF FOUND THEN
        batch_eier := current_setting('etterlatte.batch_eier', true);
        IF TG_TABLE_NAME = 'mottaker' AND TG_OP = 'UPDATE'
           AND (to_jsonb(NEW) - 'journalpost_id' - 'bestilling_id') =
               (to_jsonb(OLD) - 'journalpost_id' - 'bestilling_id') THEN
            RETURN NEW;
        END IF;
        IF TG_TABLE_NAME IN ('pdf', 'hendelse')
           AND TG_OP = 'INSERT'
           AND batch_eier = utsending.eier::TEXT THEN
            RETURN NEW;
        END IF;
        RAISE EXCEPTION 'BATCHBREV_KAN_IKKE_ENDRES' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER batchbrev_innhold BEFORE UPDATE OR DELETE ON innhold
    FOR EACH ROW EXECUTE FUNCTION beskytt_batchbrev();
CREATE TRIGGER batchbrev_mottaker BEFORE INSERT OR UPDATE OR DELETE ON mottaker
    FOR EACH ROW EXECUTE FUNCTION beskytt_batchbrev();
CREATE TRIGGER batchbrev_pdf BEFORE INSERT OR UPDATE OR DELETE ON pdf
    FOR EACH ROW EXECUTE FUNCTION beskytt_batchbrev();
CREATE TRIGGER batchbrev_brev BEFORE UPDATE OR DELETE ON brev
    FOR EACH ROW EXECUTE FUNCTION beskytt_batchbrev();
CREATE TRIGGER batchbrev_hendelse BEFORE INSERT OR UPDATE OR DELETE ON hendelse
    FOR EACH ROW EXECUTE FUNCTION beskytt_batchbrev();
