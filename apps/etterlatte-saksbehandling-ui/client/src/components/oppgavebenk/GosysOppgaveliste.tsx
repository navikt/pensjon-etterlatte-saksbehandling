import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
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

interface Props {
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const GosysOppgaveliste = ({ oppdaterTildeling, saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const hentAlleGosysOppgaver = () => {
    hentGosysOppgaverFetch({})
  }

  useEffect(() => {
    hentAlleGosysOppgaver()
  }, [])

  return mapResult(gosysOppgaverResult, {
    pending: <Spinner visible={true} label="Henter nye Gosys-oppgaver" />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys-oppgaver'}</ApiErrorAlert>,
    success: (oppgaver) => (
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
          hentAlleOppgaver={hentAlleGosysOppgaver}
          hentOppgaverStatus={() => {}}
          filter={filter}
          setFilter={setFilter}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppgavelisteValg={OppgavelisteValg.GOSYS_OPPGAVER}
        />
        <Oppgaver
          oppgaver={oppgaver}
          oppdaterTildeling={oppdaterTildeling}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          revurderingsaarsaker={new RevurderingsaarsakerDefault()}
          filter={filter}
        />
      </>
    ),
  })
}

const VisKunMineGosysOppgaverSwitch = styled(Switch)`
  margin-bottom: 0.5rem;
`
