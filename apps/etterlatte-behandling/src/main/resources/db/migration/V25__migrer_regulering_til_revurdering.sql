UPDATE behandling
    set behandlingstype = 'REVURDERING',
        revurdering_aarsak = 'REGULERING'
    where behandlingstype = 'OMREGNING'