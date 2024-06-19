-- quoted fordi case-sensitive json keys
CREATE TYPE avkortetytelse AS
(
    fom                    text,
    tom                    text,
    "type"                 text,
    "ytelseFoerAvkorting"  int,
    "ytelseEtterAvkorting" int
);


insert into avkortet_ytelse_periode
(vedtakid,
 datofom,
 datotom,
 type,
 ytelsefoer,
 ytelseetter)
SELECT v.id,
       to_date(periode.fom, 'YYYY-MM'),
       (date_trunc('month', to_date(periode.tom, 'YYYY-MM')) + interval '1 month - 1 day')::date,
       periode.type,
       periode."ytelseFoerAvkorting",
       periode."ytelseEtterAvkorting"
FROM vedtak v
   , jsonb_populate_recordset(null::avkortetytelse, v.avkorting::jsonb -> 'avkortetYtelse') periode
where v.saktype = 'OMSTILLINGSSTOENAD'
  and v.avkorting IS NOT NULL
  and not exists (select ayp
                  from avkortet_ytelse_periode ayp
                  where ayp.vedtakid = v.id)
order by v.id, periode.fom
;


DROP TYPE avkortetytelse;
