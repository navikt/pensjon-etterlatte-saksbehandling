import React, { useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { hentOppgaverMedStatus, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useAppSelector } from '~store/Store'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useApiCall } from '~shared/hooks/useApiCall'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const Oppgavelista = ({ saksbehandlereIEnhet, revurderingsaarsaker }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(hentFilterFraLocalStorage())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavebenkState.oppgavelistaOppgaver, oppgave.id, saksbehandler, versjon)
      )
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
    leggFilterILocalStorage({ ...filter, sakEllerFnrFilter: '' })
  }, [filter])

  useEffect(() => {
    if (!oppgavebenkState.oppgavelistaOppgaver?.length) {
      hentOppgavelistaOppgaver()
    }
  }, [oppgavebenkState.oppgavelistaOppgaver])

  return oppgavebenkState.oppgavelistaOppgaver.length && !isPending(oppgavelistaOppgaverResult) ? (
    <>
      <FilterRad
        hentAlleOppgaver={hentOppgavelistaOppgaver}
        hentOppgaverStatus={hentOppgavelistaOppgaver}
        filter={filter}
        setFilter={setFilter}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.OPPGAVELISTA}
      />
      <Oppgaver
        oppgaver={oppgavebenkState.oppgavelistaOppgaver}
        oppdaterTildeling={oppdaterSaksbehandlerTildeling}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        revurderingsaarsaker={revurderingsaarsaker}
      />
    </>
  ) : (
    mapResult(oppgavelistaOppgaverResult, {
      pending: <Spinner visible={true} label="Henter oppgaver" />,
      error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>,
    })
  )
}
