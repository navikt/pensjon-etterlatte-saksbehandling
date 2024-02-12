import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { useApiCall } from '~shared/hooks/useApiCall'

import {
  hentGosysOppgaver,
  OppgaveDTO,
  Saksbehandler,
  saksbehandlereIEnhetApi,
  hentOppgaverMedStatus,
} from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { FilterRad } from '~components/oppgavebenk/FilterRad'
import {
  Filter,
  filtrerOppgaver,
  filtrerOppgaveStatus,
  OPPGAVESTATUSFILTER,
} from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Tilgangsmelding } from '~components/oppgavebenk/Tilgangsmelding'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { hentFilterFraLocalStorage, leggFilterILocalStorage } from '~components/oppgavebenk/filter/filterLocalStorage'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [filter, setFilter] = useState<Filter>(hentFilterFraLocalStorage())
  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const [oppgaver, hentOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaver, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)
  const [saksbehandlereIEnhet, hentSaksbehandlereIEnhet] = useApiCall(saksbehandlereIEnhetApi)

  const [hentedeOppgaver, setHentedeOppgaver] = useState<OppgaveDTO[]>([])

  const [hentedeSaksbehandlereIEnhet, setHentedeSaksbehandlereIEnhet] = useState<Array<Saksbehandler>>([])

  const sorterOppgaverEtterOpprettet = (oppgaver: OppgaveDTO[]) => {
    return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
  }

  const hentAlleSaksbehandlereIEnhet = () => {
    innloggetSaksbehandler.enheter.map((enhet) => hentSaksbehandlereIEnhet({ enhet: enhet }))
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

  useEffect(() => {
    // Utrolig hacky måte å løse det på...
    // Problemet er at en saksbehandler kan leve i flere enhet, så for å forhindre at det blir
    // duplikate saksbehandlere, må man konvertere til et Set og konvertere det tilbake til array
    if (isSuccess(saksbehandlereIEnhet)) {
      const saksbehandlereSomJSONString: string[] = []
      saksbehandlereIEnhet.data.map((behandler) => saksbehandlereSomJSONString.push(JSON.stringify(behandler)))

      const eksisterendeSaksbehandlereSomJSONString: string[] = []
      hentedeSaksbehandlereIEnhet.map((behandler) =>
        eksisterendeSaksbehandlereSomJSONString.push(JSON.stringify(behandler))
      )
      const setAvUnikeSaksbehandlereSomStrenger = new Set(
        eksisterendeSaksbehandlereSomJSONString.concat(saksbehandlereSomJSONString)
      )

      const unikeSaksbehandlere: Saksbehandler[] = []
      setAvUnikeSaksbehandlereSomStrenger.forEach((behandler) => unikeSaksbehandlere.push(JSON.parse(behandler)))
      // Sorter liste over saksbehandlere alfabetisk
      setHentedeSaksbehandlereIEnhet(
        unikeSaksbehandlere.sort((a, b) => {
          return a.navn && b.navn ? a.navn?.localeCompare(b.navn!) : 0
        })
      )
    }
  }, [saksbehandlereIEnhet])

  useEffect(() => {
    hentAlleOppgaver()
    hentAlleSaksbehandlereIEnhet()
  }, [])

  const hentAlleOppgaver = () => {
    hentOppgaverStatusFetch({ oppgavestatusFilter: filter.oppgavestatusFilter })
    hentGosysOppgaverFunc({})
  }

  useEffect(() => {
    const statusValg =
      oppgaveListeValg === 'MinOppgaveliste'
        ? [OPPGAVESTATUSFILTER.UNDER_BEHANDLING]
        : [OPPGAVESTATUSFILTER.NY, OPPGAVESTATUSFILTER.UNDER_BEHANDLING]
    hentOppgaverStatusFetch({
      oppgavestatusFilter: statusValg,
      minOppgavelisteIdent: oppgaveListeValg === 'MinOppgaveliste',
    })

    setFilter({
      ...hentFilterFraLocalStorage(),
      oppgavestatusFilter: statusValg,
    })
  }, [oppgaveListeValg])

  useEffect(() => {
    leggFilterILocalStorage(filter)
  }, [filter])

  const oppdaterTildeling = (id: string, saksbehandler: string | null, versjon: number | null) => {
    setTimeout(() => {
      const oppdatertOppgaveState = [...hentedeOppgaver]
      const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
      oppdatertOppgaveState[index].saksbehandlerIdent = saksbehandler
      oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
      oppdatertOppgaveState[index].versjon = versjon
      setHentedeOppgaver(oppdatertOppgaveState)
    }, 2000)
  }

  const mutableOppgaver = hentedeOppgaver.concat()
  const innloggetSaksbehandleroppgaver = mutableOppgaver.filter(
    (o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident
  )

  const filtrerteOppgaver = filtrerOppgaver(
    filter.enhetsFilter,
    filter.fristFilter,
    filter.saksbehandlerFilter,
    filter.ytelseFilter,
    filter.oppgavestatusFilter,
    filter.oppgavetypeFilter,
    filter.oppgavekildeFilter,
    mutableOppgaver,
    filter.fristSortering,
    filter.fnrSortering,
    filter.fnrFilter
  )

  return (
    <Container>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label={`Oppgavelisten (${hentedeOppgaver.length})`} icon={<InboxIcon />} />
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
                hentAlleOppgaver={hentAlleOppgaver}
                hentOppgaverStatus={(oppgavestatusFilter: Array<string>) =>
                  hentOppgaverStatusFetch({ oppgavestatusFilter: oppgavestatusFilter })
                }
                filter={filter}
                setFilter={setFilter}
                alleOppgaver={hentedeOppgaver}
              />
              <Oppgavelista
                filtrerteOppgaver={filtrerteOppgaver}
                oppdaterTildeling={oppdaterTildeling}
                hentOppgaver={hentAlleOppgaver}
                saksbehandlereIEnhet={hentedeSaksbehandlereIEnhet}
                filter={filter}
                setFilter={setFilter}
                totaltAntallOppgaver={hentedeOppgaver.length}
                erMinOppgaveliste={false}
              />
            </>
          )}
          {oppgaveListeValg === 'MinOppgaveliste' && (
            <>
              <ValgWrapper>
                <VelgOppgavestatuser
                  value={filter.oppgavestatusFilter}
                  onChange={(oppgavestatusFilter) => {
                    hentOppgaverStatusFetch({
                      oppgavestatusFilter: oppgavestatusFilter,
                      minOppgavelisteIdent: true,
                    })
                    setFilter({ ...filter, oppgavestatusFilter })
                  }}
                />
              </ValgWrapper>
              <Oppgavelista
                filtrerteOppgaver={filtrerOppgaveStatus(filter.oppgavestatusFilter, innloggetSaksbehandleroppgaver)}
                hentOppgaver={hentAlleOppgaver}
                saksbehandlereIEnhet={hentedeSaksbehandlereIEnhet}
                filter={filter}
                setFilter={setFilter}
                oppdaterTildeling={(id, _saksbehandler, versjon) => oppdaterTildeling(id, null, versjon)}
                erMinOppgaveliste={true}
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
