
-- alle aktivitetsgrader har tom = fom, når de i stedet skulle være tom = null for dataene vi har i løsningen
update aktivitetsplikt
set aktivitetsgrad = (SELECT jsonb_agg(
                                     jsonb_set(ag, '{tom}', 'null', false)
                             )
                      FROM jsonb_array_elements(aktivitetsgrad)  ag)
where jsonb_array_length(aktivitetsgrad) > 0;
