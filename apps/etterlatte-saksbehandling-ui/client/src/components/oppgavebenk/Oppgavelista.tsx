import React, { useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { hentOppgaverMedStatus } from '~shared/api/oppgaver'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  leggTilOppgavenIMinliste,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Saksbehandler } from '~shared/types/saksbehandler'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import {
  hentOppgavelistenFilterFraLocalStorage,
  leggOppgavelistenFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const Oppgavelista = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [filter, setFilter] = useState<Filter>(hentOppgavelistenFilterFraLocalStorage())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavebenkState.oppgavelistaOppgaver, oppgave.id, saksbehandler)
      )
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            leggTilOppgavenIMinliste(oppgavebenkState.minOppgavelisteOppgaver, oppgave, saksbehandler)
          )
        )
      } else {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            filtrerKunInnloggetBrukerOppgaver(
              finnOgOppdaterSaksbehandlerTildeling(oppgavebenkState.minOppgavelisteOppgaver, oppgave.id, saksbehandler)
            )
          )
        )
      }
    }, 2000)
  }

  const hentOppgavelistaOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentOppgavelistaOppgaverFetch(
      {
        oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : filter.oppgavestatusFilter,
        minOppgavelisteIdent: false,
      },
      (oppgaver) => {
        dispatcher.setOppgavelistaOppgaver(sorterOppgaverEtterOpprettet(oppgaver))
      }
    )

  useEffect(() => {
    leggOppgavelistenFilterILocalStorage({ ...filter, sakEllerFnrFilter: '' })
  }, [filter])

  useEffect(() => {
    if (!oppgavebenkState.oppgavelistaOppgaver?.length) {
      hentOppgavelistaOppgaver()
    }
  }, [])

  return (
    <>
      <FilterRad
        hentAlleOppgaver={hentOppgavelistaOppgaver}
        hentOppgaverStatus={hentOppgavelistaOppgaver}
        filter={filter}
        setFilter={setFilter}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.OPPGAVELISTA}
      />

      {oppgavebenkState.oppgavelistaOppgaver.length >= 0 && !isPending(oppgavelistaOppgaverResult) ? (
        <Oppgaver
          oppgaver={oppgavebenkState.oppgavelistaOppgaver}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
          filter={filter}
        />
      ) : (
        mapResult(oppgavelistaOppgaverResult, {
          pending: <Spinner visible={true} label="Henter oppgaver" />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>,
        })
      )}
    </>
  )
}
