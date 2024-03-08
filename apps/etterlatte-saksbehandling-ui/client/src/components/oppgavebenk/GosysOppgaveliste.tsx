import React, { useEffect, useState } from 'react'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
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
import { ApiError } from '~shared/api/apiClient'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  gosysOppgaverResult: Result<OppgaveDTO[]>
  hentGosysOppgaverFetch: (
    args: unknown,
    onSuccess?: ((result: OppgaveDTO[], statusCode: number) => void) | undefined,
    onError?: ((error: ApiError) => void) | undefined
  ) => void
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet, gosysOppgaverResult, hentGosysOppgaverFetch }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)

  const [oppgaver, setOppgaver] = useState<Array<OppgaveDTO>>([])

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      setOppgaver(finnOgOppdaterSaksbehandlerTildeling(oppgaver, oppgave.id, saksbehandler, versjon))
    }, 2000)
  }

  useEffect(() => {
    if (isSuccess(gosysOppgaverResult)) {
      setOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaverResult.data))
    }
  }, [gosysOppgaverResult])

  useEffect(() => {
    if (!oppgaver?.length) hentGosysOppgaverFetch({})
  }, [])

  return mapResult(gosysOppgaverResult, {
    pending: <Spinner visible={true} label="Henter nye Gosys-oppgaver" />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys-oppgaver'}</ApiErrorAlert>,
    success: () => (
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
          oppgaver={oppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
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
