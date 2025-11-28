
-- View for forrige maanedes statistikk for import (vi vil kun produsere etter maaneden er ferdig)
-- oppdaterer viewet med nye kolonner for etteroppgjøret
CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS SELECT * FROM maaned_stoenad
    WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());
