ALTER TABLE vilkaar ADD COLUMN kopiert_fra_vilkaar UUID;
ALTER TABLE vilkaar ADD COLUMN grunnlag_versjon INTEGER;

-- Oppdaterer grunnlag_versjon i vilkaar med verdien som ligger i vilkaarsvurdering.
-- Kun relevant for vilkaar med tilknyttet grunnlag.
UPDATE vilkaar
SET grunnlag_versjon = subquery.grunnlag_versjon
FROM (SELECT vv.id, vv.grunnlag_versjon
      FROM vilkaarsvurdering vv,
           vilkaarsvurdering_kilde
      WHERE vv.id = vilkaarsvurdering_id) AS subquery
WHERE vilkaarsvurdering_id = subquery.id
  AND vilkaar.id IN (SELECT vilkaar_id FROM grunnlag);

-- Oppdaterer kopiert_fra_vilkaar med verdi fra eksisterende vilkår som er kopiert fra en tidligere vilkårvurdering.
-- Vilkårene kobles basert på vilkaar_type for hovedvilkår samt koblingstabellen vilkaarsvurdering_kilde
WITH subquery AS (
    SELECT DISTINCT vilkaarKopi.id       AS vilkaarKopiId,
                    vilkaarKopiertFra.id AS vilkaarKopiertId
    FROM vilkaar vilkaarKopi
             INNER JOIN delvilkaar dv1 ON vilkaarKopi.id = dv1.vilkaar_id AND dv1.hovedvilkaar = true AND
                                          vilkaarKopi.kopiert_fra_vilkaar is null
             INNER JOIN vilkaarsvurdering_kilde vk ON vilkaarKopi.vilkaarsvurdering_id = vk.vilkaarsvurdering_id
             INNER JOIN vilkaar vilkaarKopiertFra
                        ON vk.kopiert_fra_vilkaarsvurdering_id = vilkaarKopiertFra.vilkaarsvurdering_id
             INNER JOIN delvilkaar dv2 ON vilkaarKopiertFra.id = dv2.vilkaar_id AND dv2.hovedvilkaar = true AND
                                          dv1.vilkaar_type = dv2.vilkaar_type
)
UPDATE vilkaar
SET kopiert_fra_vilkaar = subquery.vilkaarKopiertId
FROM subquery
WHERE vilkaar.id = subquery.vilkaarKopiId;