import React, { useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { hentOppgaverMedStatus } from '~shared/api/oppgaver'
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
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const MinOppgaveliste = ({ saksbehandlereIEnhet, revurderingsaarsaker }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [filter, setFilter] = useState<Filter>(minOppgavelisteFiltre())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavebenkState.oppgavelistaOppgaver, oppgave.id, saksbehandler, versjon)
      )
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            leggTilOppgavenIMinliste(oppgavebenkState.minOppgavelisteOppgaver, oppgave, saksbehandler, versjon)
          )
        )
      } else {
        dispatcher.setMinOppgavelisteOppgaver(
          sorterOppgaverEtterOpprettet(
            filtrerKunInnloggetBrukerOppgaver(
              finnOgOppdaterSaksbehandlerTildeling(
                oppgavebenkState.minOppgavelisteOppgaver,
                oppgave.id,
                saksbehandler,
                versjon
              )
            )
          )
        )
      }
    }, 2000)
  }

  const hentMinOppgavelisteOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentMinOppgavelisteOppgaverFetch(
      {
        oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : filter.oppgavestatusFilter,
        minOppgavelisteIdent: true,
      },
      (oppgaver) => dispatcher.setMinOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(oppgaver))
    )

  useEffect(() => {
    if (!oppgavebenkState.minOppgavelisteOppgaver?.length) hentMinOppgavelisteOppgaver()
  }, [])

  return oppgavebenkState.minOppgavelisteOppgaver.length >= 0 && !isPending(minOppgavelisteOppgaverResult) ? (
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
        oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
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
        filter={filter}
      />
    </>
  ) : (
    mapResult(minOppgavelisteOppgaverResult, {
      pending: <Spinner visible={true} label="Henter dine oppgaver" />,
      error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente dine oppgaver'}</ApiErrorAlert>,
    })
  )
}
