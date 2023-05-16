alter table bp_beregningsgrunnlag
    add column soesken_med_i_beregning_perioder jsonb;

update bp_beregningsgrunnlag skal_oppdateres
set soesken_med_i_beregning_perioder = jsonb_build_array(
        jsonb_build_object(
                'fom', pe.datofom,
                'data', data_for_oppdatering.soesken_med_i_beregning::jsonb
            )
    )
from bp_beregningsgrunnlag as data_for_oppdatering
         inner join (select sakid, min (datofom) as datofom from beregningsperiode group by sakid) as pe
                    on pe.sakid = (select distinct sakid
                                   from beregningsperiode
                                   where behandlingid = data_for_oppdatering.behandlings_id)
where data_for_oppdatering.behandlings_id = skal_oppdateres.behandlings_id;
