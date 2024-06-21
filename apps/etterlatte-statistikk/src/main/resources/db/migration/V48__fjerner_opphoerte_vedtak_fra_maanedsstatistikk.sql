delete
from maaned_stoenad ms
where id in (select m.id
             from maaned_stoenad m
                      left outer join stoenad s on m.behandlingid = s.behandlingid
             where s.opphoerfom is not null
               and s.opphoerfom <= date(statistikkmaaned || '-01'::text));
