UPDATE avstemming avs
SET loepende_utbetalinger = (SELECT jsonb_agg(
                                            jsonb_set(
                                                    ub.value,
                                                    '{utbetalingslinjer}',
                                                    (SELECT jsonb_agg(
                                                                        ubl || jsonb_build_object('kjoereplan',
                                                                                                  case ubl.value ->> 'kjoereplan'
                                                                                                      when 'MED_EN_GANG'
                                                                                                          then 'MED_EN_GANG'
                                                                                                      when 'NESTE_PLANLAGTE_UTBETALING'
                                                                                                          then 'NESTE_PLANLAGTE_UTBETALING'
                                                                                                      else 'MED_EN_GANG' end)
                                                                )
                                                     FROM jsonb_array_elements(ub.value -> 'utbetalingslinjer') ubl)
                                                )
                                        )::text
                             FROM jsonb_array_elements(avs.loepende_utbetalinger::jsonb) ub);