import React, { useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Switch } from '@navikt/ds-react'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import styled from 'styled-components'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { GosysOppgaver } from '~components/oppgavebenk/gosys/GosysOppgaver'
import { hentGosysOppgaver } from '~shared/api/gosys'
import { GosysFilterRad } from './filtreringAvOppgaver/GosysFilterRad'
import { GosysOppgave } from '~shared/types/Gosys'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

const sorterOppgaverEtterOpprettet = (oppgaver: GosysOppgave[]) => {
  return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  if (!innloggetSaksbehandler.skriveEnheter.length) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<GosysFilter>({})
  const [fnrFilter, setFnrFilter] = useState<string>()

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const hentOppgaver = (filter: GosysFilter) => {
    hentGosysOppgaverFetch(filter, (oppgaver) => {
      dispatcher.setGosysOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(oppgaver))
    })
  }

  useEffect(() => {
    hentOppgaver(filter)
  }, [filter])

  return (
    <>
      <VisKunMineGosysOppgaverSwitch
        checked={filter.saksbehandlerFilter === innloggetSaksbehandler.ident}
        onChange={(e) =>
          setFilter({
            ...filter,
            saksbehandlerFilter: e.target.checked ? innloggetSaksbehandler.ident : undefined,
          })
        }
      >
        Vis mine Gosys-oppgaver
      </VisKunMineGosysOppgaverSwitch>

      <GosysFilterRad
        hentAlleOppgaver={() => hentOppgaver(filter)}
        filter={filter}
        setFilter={setFilter}
        filterFoedselsnummer={setFnrFilter}
      />

      {mapResult(gosysOppgaverResult, {
        pending: <Spinner label="Henter Gosys-oppgaver" visible />,
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys-oppgaver'}</ApiErrorAlert>,
        success: () => (
          <GosysOppgaver
            oppgaver={oppgavebenkState.gosysOppgavelisteOppgaver}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            fnrFilter={fnrFilter}
          />
        ),
      })}
    </>
  )
}

const VisKunMineGosysOppgaverSwitch = styled(Switch)`
  margin-bottom: 0.5rem;
`
