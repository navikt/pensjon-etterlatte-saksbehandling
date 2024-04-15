import React, { useEffect, useState } from 'react'
import { hentGosysOppgaver } from '~shared/api/oppgaver'
import { isPending, mapResult } from '~shared/api/apiUtils'
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
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import styled from 'styled-components'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { MultiSelectFilter } from './filtreringAvOppgaver/MultiSelectFilter'
import { GOSYS_TEMA_FILTER, GosysTema, konverterStringTilGosysTema } from '~shared/types/Gosys'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  if (!innloggetSaksbehandler.skriveEnheter.length) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)
  const [temaFilter, setTemaFilter] = useState<GosysTema[]>([GosysTema.EYB, GosysTema.EYO])

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

  const hentOppgaver = (tema: GosysTema[]) => {
    hentGosysOppgaverFetch(tema, (oppgaver) => {
      dispatcher.setGosysOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(oppgaver))
    })
  }

  useEffect(() => {
    if (!oppgavebenkState.gosysOppgavelisteOppgaver?.length) {
      hentOppgaver(temaFilter)
    }
  }, [])

  useEffect(() => {
    hentOppgaver(temaFilter)
  }, [temaFilter])

  return oppgavebenkState.gosysOppgavelisteOppgaver.length >= 0 && !isPending(gosysOppgaverResult) ? (
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
        hentAlleOppgaver={() => hentOppgaver(temaFilter)}
        hentOppgaverStatus={() => {}}
        filter={filter}
        setFilter={setFilter}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.GOSYS_OPPGAVER}
      >
        <MultiSelectFilter
          label="Tema"
          values={temaFilter.map((tema) => GOSYS_TEMA_FILTER[tema])}
          options={Object.entries(GOSYS_TEMA_FILTER).map(([, beskrivelse]) => beskrivelse)}
          onChange={(temaer) => setTemaFilter(temaer.map(konverterStringTilGosysTema))}
        />
      </FilterRad>
      <Oppgaver
        oppgaver={oppgavebenkState.gosysOppgavelisteOppgaver}
        oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
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
