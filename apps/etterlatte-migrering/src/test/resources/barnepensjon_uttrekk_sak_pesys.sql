select v.SAK_ID,
       b.BRUTTO,
       b.NETTO,
       b.TT_ANV,
       b.DATO_VIRK_FOM,
       fvd.DATO_VIRK,      -- kan være per kravlinjetype, sjekk om relevant for bp. // dobbeltsjekk at det ikke finnes bp saker med mer enn en første virk
       b.G,
       b.K_RESULTAT_T,
       b.k_BEREG_METODE_T,
       b.K_RESULTAT_KILDE, -- preg bestemt beløp = auto, eller saksbehandler overstyrt = manuell overstyring
       k.K_KRAV_VELG_T,
       poe.ORG_ENHET_ID_FK,
       pd.K_GRNL_ROLLE_T,
       p.FNR_FK,
       pg.PERSON_GRUNNLAG_ID,
       pg.DATO_DOD         -- for å kunne legge inn fremtidig trygdetid.
       -- k.* vurdere å se på ukjent_mor/far/avdoed
from pen.T_vedtak v
         inner join PEN.T_SAK s on s.SAK_ID = v.SAK_ID
         inner join pen.T_BEREGNING b on v.VEDTAK_ID = b.VEDTAK_ID --and b.total_vinner = '1'
         inner join pen.T_SAK_TILGANG st on s.SAK_ID = st.SAK_ID AND st.K_TILGANG_T = 'PERM' AND st.DATO_TOM is null
         inner join pen.T_PEN_ORG_ENHET poe on poe.PEN_ORG_ENHET_ID = st.PEN_ORG_ENHET_ID
         inner join PEN.T_FORST_VIRK_DATO fvd on fvd.SAK_ID = s.SAK_ID and b.DATO_VIRK_FOM < current_date and
                                                 (b.DATO_VIRK_TOM is null or b.DATO_VIRK_TOM > current_date) -- and b.virk_tom is null
         inner join pen.T_KRAVHODE k on k.KRAVHODE_ID = v.KRAVHODE_ID
         inner join PEN.T_PERSON_GRUNNLAG pg on pg.KRAVHODE_ID = k.KRAVHODE_ID
         inner join PEN.T_PERSON_DET pd on pd.PERSON_GRUNNLAG_ID = pg.PERSON_GRUNNLAG_ID and pd.bruk = '1' and
                                           pd.DATO_TOM is null --and pd.K_GRNL_ROLLE_T = 'SOKER'
         inner join PEN.T_PERSON p on p.PERSON_ID = pg.PERSON_ID -- dødsdato skal være lik det som ligger i pg
where v.K_SAK_T = 'BARNEP'
  and v.DATO_LOPENDE_FOM is not null
  and v.DATO_LOPENDE_TOM is null
  and v.SAK_ID in (<SAK_ID>)
  and s.K_UTLANDSTILKNYTNING = 'NASJONAL';

-- hent trygdetid separat
-- Hentes fra avdødes persongrunnlag
select land.LAND_3_TEGN,
       tg.TRYGDETID_GRNL_ID,
       tg.PERSON_GRUNNLAG_ID,
       tg.DATO_FOM,
       tg.DATO_TOM,
       tg.POENG_I_INN_AR,
       tg.POENG_I_UT_AR,
       tg.IKKE_PRO_RATA,
       t.TT_FAKTISK,
       t.FTT,
       t.TT_ANV
from PEN.T_TRYGDETID_GRNL tg -- sjekk ut grunnlag_vedlegg_id? Kanskje ta med kilde som begrunnelse.
         inner join PEN.T_K_LAND_3_TEGN land on tg.K_LAND_3_TEGN_ID = land.K_LAND_3_TEGN_ID
         inner join PEN.T_TRYGDETID t on t.PERSON_GRUNNLAG_ID = tg.PERSON_GRUNNLAG_ID
where tg.PERSON_GRUNNLAG_ID = <AVDOED_PERSON_GRUNNLAG_ID>;