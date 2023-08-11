DO $$
    DECLARE
        id_value uuid;
    BEGIN
        FOR id_value IN (SELECT DISTINCT vilkaar.id FROM vilkaar, delvilkaar WHERE vilkaar.id = delvilkaar.vilkaar_id AND delvilkaar.vilkaar_type = 'OMS_GJENLEVENDES_MEDLEMSKAP')
            LOOP
                INSERT INTO delvilkaar (vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat, spoersmaal) VALUES (id_value, 'OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_YRKESSKADE', false, 'Ja, dødsfallet skyldes en godkjent yrkes-skade/sykdom', null, '§ 17-2', null, null, null, null, null);
            END LOOP;
    END $$;


