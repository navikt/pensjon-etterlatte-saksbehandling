import React, { useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
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
import { ApiError } from '~shared/api/apiClient'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
  minOppgavelisteOppgaverResult: Result<OppgaveDTO[]>
  hentMinOppgavelisteOppgaverFetch: (
    args: { oppgavestatusFilter: string[]; minOppgavelisteIdent?: boolean | undefined },
    onSuccess?: ((result: OppgaveDTO[], statusCode: number) => void) | undefined,
    onError?: ((error: ApiError) => void) | undefined
  ) => void
}

export const MinOppgaveliste = ({
  saksbehandlereIEnhet,
  revurderingsaarsaker,
  minOppgavelisteOppgaverResult,
  hentMinOppgavelisteOppgaverFetch,
}: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(minOppgavelisteFiltre())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        dispatcher.setMinOppgavelisteOppgaver(
          leggTilOppgavenIMinliste(oppgavebenkState.minOppgavelisteOppgaver, oppgave, saksbehandler, versjon)
        )
      } else {
        dispatcher.setMinOppgavelisteOppgaver(
          filtrerKunInnloggetBrukerOppgaver(
            finnOgOppdaterSaksbehandlerTildeling(
              oppgavebenkState.minOppgavelisteOppgaver,
              oppgave.id,
              saksbehandler,
              versjon
            )
          )
        )
      }
    }, 2000)
  }

  const hentMinOppgavelisteOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentMinOppgavelisteOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : filter.oppgavestatusFilter,
      minOppgavelisteIdent: true,
    })

  useEffect(() => {
    if (!oppgavebenkState.minOppgavelisteOppgaver?.length) hentMinOppgavelisteOppgaver()
  }, [])

  useEffect(() => {
    if (isSuccess(minOppgavelisteOppgaverResult)) {
      dispatcher.setMinOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(minOppgavelisteOppgaverResult.data))
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
          oppgaver={oppgavebenkState.minOppgavelisteOppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
            oppdaterFrist(
              dispatcher.setMinOppgavelisteOppgaver,
              oppgavebenkState.minOppgavelisteOppgaver,
              id,
              nyfrist,
              versjon
            )
          }
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          revurderingsaarsaker={revurderingsaarsaker}
        />
      </>
    ),
  })
}
