update klage
set utfall = jsonb_set(utfall #- '{innstilling,tekst}',
                       '{innstilling,internKommentar}',
                       utfall #> '{innstilling,tekst}')
where utfall #> '{innstilling}' ? 'tekst';