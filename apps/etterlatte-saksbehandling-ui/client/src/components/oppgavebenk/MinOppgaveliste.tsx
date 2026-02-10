import React, { useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { hentOppgaverMedStatus } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import {
  finnOgOppdaterOppgave,
  leggTilOppgavenIMinliste,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import {
  hentMinOppgavelisteFilterFraLocalStorage,
  leggMinOppgavelisteFilterILocalsotrage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { VStack } from '@navikt/ds-react'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const MinOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [filter, setFilter] = useState<Filter>(hentMinOppgavelisteFilterFraLocalStorage())

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.oppgavelistaOppgaver, oppgave.id, {
          status: Oppgavestatus.UNDER_BEHANDLING,
          saksbehandler,
        })
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
              finnOgOppdaterOppgave(oppgavebenkState.minOppgavelisteOppgaver, oppgave.id, {
                status: Oppgavestatus.UNDER_BEHANDLING,
                saksbehandler,
              })
            )
          )
        )
      }
    }, 2000)
  }

  const oppdaterStatus = (oppgaveId: string, status: Oppgavestatus) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.oppgavelistaOppgaver, oppgaveId, { status })
      )
      dispatcher.setMinOppgavelisteOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.minOppgavelisteOppgaver, oppgaveId, { status })
      )
    }, 2000)
  }

  const oppdaterFrist = (oppgaveId: string, frist: string) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.oppgavelistaOppgaver, oppgaveId, { frist })
      )
      dispatcher.setMinOppgavelisteOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.minOppgavelisteOppgaver, oppgaveId, { frist })
      )
    }, 2000)
  }

  const oppdaterMerknad = (oppgaveId: string, merknad: string) => {
    setTimeout(() => {
      dispatcher.setOppgavelistaOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.oppgavelistaOppgaver, oppgaveId, { merknad })
      )
      dispatcher.setMinOppgavelisteOppgaver(
        finnOgOppdaterOppgave(oppgavebenkState.minOppgavelisteOppgaver, oppgaveId, { merknad })
      )
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
    leggMinOppgavelisteFilterILocalsotrage(filter)
  }, [filter])

  useEffect(() => {
    if (!oppgavebenkState.minOppgavelisteOppgaver?.length) hentMinOppgavelisteOppgaver()
  }, [])

  return (
    <VStack gap="space-6">
      <FilterRad
        hentAlleOppgaver={hentMinOppgavelisteOppgaver}
        filter={filter}
        setFilter={setFilter}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.MIN_OPPGAVELISTE}
      />

      {oppgavebenkState.minOppgavelisteOppgaver.length >= 0 && !isPending(minOppgavelisteOppgaverResult) ? (
        <Oppgaver
          oppgaver={oppgavebenkState.minOppgavelisteOppgaver}
          oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={oppdaterFrist}
          oppdaterStatus={oppdaterStatus}
          oppdaterMerknad={oppdaterMerknad}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          filter={filter}
        />
      ) : (
        mapResult(minOppgavelisteOppgaverResult, {
          pending: <Spinner label="Henter dine oppgaver" />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente dine oppgaver'}</ApiErrorAlert>,
        })
      )}
    </VStack>
  )
}
