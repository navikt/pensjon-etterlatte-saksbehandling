import React, { useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedStatus, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { minOppgavelisteFiltre } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  leggTilOppgavenIMinliste,
  oppdaterFrist,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const MinOppgaveliste = ({ saksbehandlereIEnhet, revurderingsaarsaker }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(minOppgavelisteFiltre())

  const [oppgaver, setOppgaver] = useState<Array<OppgaveDTO>>([])

  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        setOppgaver(leggTilOppgavenIMinliste(oppgaver, oppgave, saksbehandler, versjon))
      } else {
        setOppgaver(finnOgOppdaterSaksbehandlerTildeling(oppgaver, oppgave.id, saksbehandler, versjon))
      }
    }, 2000)
  }

  const hentMinOppgavelisteOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentMinOppgavelisteOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : filter.oppgavestatusFilter,
      minOppgavelisteIdent: true,
    })

  useEffect(() => {
    hentMinOppgavelisteOppgaver()
  }, [])

  useEffect(() => {
    if (isSuccess(minOppgavelisteOppgaverResult)) {
      setOppgaver(sorterOppgaverEtterOpprettet(minOppgavelisteOppgaverResult.data))
    }
  }, [minOppgavelisteOppgaverResult])

  return mapResult(minOppgavelisteOppgaverResult, {
    pending: <Spinner visible={true} label="Henter dine oppgaver" />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente dine oppgaver'}</ApiErrorAlert>,
    success: () => (
      <>
        <FilterRad
          hentAlleOppgaver={hentMinOppgavelisteOppgaver}
          hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => hentMinOppgavelisteOppgaver(oppgavestatusFilter)}
          filter={filter}
          setFilter={setFilter}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppgavelisteValg={OppgavelisteValg.MIN_OPPGAVELISTE}
        />
        <Oppgaver
          oppgaver={oppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
            oppdaterFrist(setOppgaver, oppgaver, id, nyfrist, versjon)
          }
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          revurderingsaarsaker={revurderingsaarsaker}
        />
      </>
    ),
  })
}
