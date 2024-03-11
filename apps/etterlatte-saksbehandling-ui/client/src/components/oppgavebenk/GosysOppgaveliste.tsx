import React, { useEffect, useState } from 'react'
import { hentGosysOppgaver, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerDefault } from '~shared/types/Revurderingaarsak'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Filter, SAKSBEHANDLERFILTER } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { defaultFiltre } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Switch } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import styled from 'styled-components'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      dispatcher.setGosysOppgavelisteOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(
          oppgavebenkState.gosysOppgavelisteOppgaver,
          oppgave.id,
          saksbehandler,
          versjon
        )
      )
    }, 2000)
  }

  useEffect(() => {
    if (!oppgavebenkState.gosysOppgavelisteOppgaver?.length) {
      hentGosysOppgaverFetch({}, (oppgaver) =>
        dispatcher.setGosysOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(oppgaver))
      )
    }
  }, [oppgavebenkState.gosysOppgavelisteOppgaver])

  return oppgavebenkState.gosysOppgavelisteOppgaver.length ? (
    <>
      <VisKunMineGosysOppgaverSwitch
        checked={filter.saksbehandlerFilter === innloggetSaksbehandler.ident}
        onChange={(e) =>
          setFilter({
            ...filter,
            saksbehandlerFilter: e.target.checked ? innloggetSaksbehandler.ident : SAKSBEHANDLERFILTER.visAlle,
          })
        }
      >
        Vis mine Gosys-oppgaver
      </VisKunMineGosysOppgaverSwitch>
      <FilterRad
        hentAlleOppgaver={() => hentGosysOppgaverFetch({})}
        hentOppgaverStatus={() => {}}
        filter={filter}
        setFilter={setFilter}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.GOSYS_OPPGAVER}
      />
      <Oppgaver
        oppgaver={oppgavebenkState.gosysOppgavelisteOppgaver}
        oppdaterTildeling={oppdaterSaksbehandlerTildeling}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        revurderingsaarsaker={new RevurderingsaarsakerDefault()}
        filter={filter}
      />
    </>
  ) : (
    mapResult(gosysOppgaverResult, {
      pending: <Spinner visible={true} label="Henter nye Gosys-oppgaver" />,
      error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys-oppgaver'}</ApiErrorAlert>,
    })
  )
}

const VisKunMineGosysOppgaverSwitch = styled(Switch)`
  margin-bottom: 0.5rem;
`
