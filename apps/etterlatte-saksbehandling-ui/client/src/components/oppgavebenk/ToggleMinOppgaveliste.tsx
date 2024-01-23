import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaver, OppgaveDTO } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { FilterRad } from '~components/oppgavebenk/FilterRad'
import {
  Filter,
  filtrerOppgaver,
  filtrerOppgaveStatus,
  initialFilter,
  OPPGAVESTATUSFILTER,
} from '~components/oppgavebenk/oppgavelistafiltre'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Tilgangsmelding } from '~components/oppgavebenk/Tilgangsmelding'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }
  const [filter, setFilter] = useState<Filter>(initialFilter())
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaverFetch] = useApiCall(hentOppgaver)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const [hentedeOppgaver, setHentedeOppgaver] = useState<OppgaveDTO[]>([])

  const sorterOppgaverEtterOpprettet = (oppgaver: OppgaveDTO[]) => {
    return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
  }
  const hentAlleOppgaver = () => {
    hentOppgaverFetch({})
    hentGosysOppgaverFunc({})
  }

  useEffect(() => {
    if (isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      const alleOppgaver = sorterOppgaverEtterOpprettet([...oppgaver.data, ...gosysOppgaver.data])
      setHentedeOppgaver(alleOppgaver)
    } else if (isSuccess(oppgaver) && !isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(oppgaver.data))
    } else if (!isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaver.data))
    }
  }, [oppgaver, gosysOppgaver])

  useEffect(() => hentAlleOppgaver(), [])

  // Dette er bare for å resette filterne når saksbehandler bytter mellom sin egen
  // og den felles oppgavelisten. Vet egt ikke helt om dette er ønsket behaviour
  useEffect(() => {
    if (oppgaveListeValg === 'MinOppgaveliste') {
      setFilter({ ...initialFilter(), oppgavestatusFilter: [OPPGAVESTATUSFILTER.UNDER_BEHANDLING] })
    } else {
      setFilter(initialFilter())
    }
  }, [oppgaveListeValg])

  const oppdaterTildeling = (id: string, saksbehandler: string | null, versjon: number | null) => {
    setTimeout(() => {
      const oppdatertOppgaveState = [...hentedeOppgaver]
      const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
      oppdatertOppgaveState[index].saksbehandler = saksbehandler
      oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
      oppdatertOppgaveState[index].versjon = versjon
      setHentedeOppgaver(oppdatertOppgaveState)
    }, 2000)
  }

  const mutableOppgaver = hentedeOppgaver.concat()
  const innloggetSaksbehandleroppgaver = mutableOppgaver.filter((o) => o.saksbehandler === innloggetSaksbehandler.ident)
  const filtrerteOppgaver = filtrerOppgaver(
    filter.enhetsFilter,
    filter.fristFilter,
    filter.saksbehandlerFilter,
    filter.ytelseFilter,
    filter.oppgavestatusFilter,
    filter.oppgavetypeFilter,
    filter.oppgavekildeFilter,
    mutableOppgaver,
    filter.fnrFilter
  )

  return (
    <Container>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label="Oppgavelisten" icon={<InboxIcon />} />
          <Tabs.Tab
            value="MinOppgaveliste"
            label={`Min oppgaveliste (${innloggetSaksbehandleroppgaver.length})`}
            icon={<PersonIcon aria-hidden />}
          />
        </Tabs.List>
      </TabsWidth>

      {isPending(oppgaver) && <Spinner visible={true} label="Henter nye oppgaver" />}
      {isFailureHandler({
        apiResult: oppgaver,
        errorMessage: 'Kunne ikke hente oppgaver',
      })}
      {isFailureHandler({
        apiResult: gosysOppgaver,
        errorMessage: 'Kunne ikke hente gosys oppgaver',
      })}
      {isSuccess(oppgaver) && (
        <>
          {oppgaveListeValg === 'Oppgavelista' && (
            <>
              <FilterRad
                hentOppgaver={hentAlleOppgaver}
                filter={filter}
                setFilter={setFilter}
                alleOppgaver={hentedeOppgaver}
              />
              <Oppgavelista
                filtrerteOppgaver={filtrerteOppgaver}
                oppdaterTildeling={oppdaterTildeling}
                hentOppgaver={hentAlleOppgaver}
                totaltAntallOppgaver={hentedeOppgaver.length}
              />
            </>
          )}
          {oppgaveListeValg === 'MinOppgaveliste' && (
            <>
              <ValgWrapper>
                <VelgOppgavestatuser
                  value={filter.oppgavestatusFilter}
                  onChange={(oppgavestatusFilter) => setFilter({ ...filter, oppgavestatusFilter })}
                />
              </ValgWrapper>
              <Oppgavelista
                filtrerteOppgaver={filtrerOppgaveStatus(filter.oppgavestatusFilter, innloggetSaksbehandleroppgaver)}
                hentOppgaver={hentAlleOppgaver}
                oppdaterTildeling={(id, _saksbehandler, versjon) => oppdaterTildeling(id, null, versjon)}
              />
            </>
          )}
        </>
      )}
    </Container>
  )
}

const ValgWrapper = styled.div`
  margin-bottom: 2rem;
  width: 35rem;
`

const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
