SELECT vv.behandling_id
INTO TABLE migrert_yrkesskade
FROM delvilkaar d
         INNER JOIN vilkaar v ON v.id = d.vilkaar_id AND v.resultat_saksbehandler = 'Pesys'
         INNER JOIN vilkaarsvurdering vv ON v.vilkaarsvurdering_id = vv.id
WHERE d.vilkaar_type = 'BP_YRKESSKADE_AVDOED_2024'
  AND d.resultat = 'OPPFYLT';