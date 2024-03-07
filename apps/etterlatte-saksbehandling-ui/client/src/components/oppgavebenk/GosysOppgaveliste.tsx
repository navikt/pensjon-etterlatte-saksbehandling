import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { defaultFiltre } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Switch } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import styled from 'styled-components'

interface Props {
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const GosysOppgaveliste = ({ oppdaterTildeling, saksbehandlereIEnhet, revurderingsaarsaker }: Props) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(defaultFiltre)

  const [visKunMineGosysOppgaver, setVisKunMineGosysOppgaver] = useState<boolean>(false)

  const [gosysOppgaver, setGosysOppgaver] = useState<Array<OppgaveDTO>>([])

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const kunMineGosysOppgaver = (): Array<OppgaveDTO> => {
    return gosysOppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const hentAlleGosysOppgaver = () => {
    hentGosysOppgaverFetch({})
  }

  useEffect(() => {
    hentAlleGosysOppgaver()
  }, [])

  useEffect(() => {
    if (isSuccess(gosysOppgaverResult)) setGosysOppgaver(gosysOppgaverResult.data)
  }, [gosysOppgaverResult])

  return mapApiResult(
    gosysOppgaverResult,
    <Spinner visible={true} label="Henter nye Gosys oppgaver" />,
    (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys oppgaver'}</ApiErrorAlert>,
    () => (
      <>
        <VisKunMineGosysOppgaverSwitch
          checked={visKunMineGosysOppgaver}
          onChange={(e) => {
            setVisKunMineGosysOppgaver(e.target.checked)
          }}
        >
          Vis mine Gosys oppgaver
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
          oppgaver={visKunMineGosysOppgaver ? kunMineGosysOppgaver() : gosysOppgaver}
          oppdaterTildeling={oppdaterTildeling}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          revurderingsaarsaker={revurderingsaarsaker}
          filter={filter}
        />
      </>
    )
  )
}

const VisKunMineGosysOppgaverSwitch = styled(Switch)`
  margin-bottom: 0.5rem;
`
