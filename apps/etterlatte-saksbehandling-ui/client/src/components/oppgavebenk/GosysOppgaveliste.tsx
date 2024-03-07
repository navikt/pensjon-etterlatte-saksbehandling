import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
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
  OppgavelisteneStats,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppgavelisteneStats: OppgavelisteneStats
  setOppgavelisteneStats: Dispatch<SetStateAction<OppgavelisteneStats>>
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet, oppgavelisteneStats, setOppgavelisteneStats }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)

  const [oppgaver, setOppgaver] = useState<Array<OppgaveDTO>>([])

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

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
      setOppgavelisteneStats({
        ...oppgavelisteneStats,
        antallGosysOppgaver: gosysOppgaverResult.data.length,
      })
    }
  }, [gosysOppgaverResult])

  useEffect(() => {
    hentGosysOppgaverFetch({})
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
