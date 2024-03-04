import React, { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Container, FlexRow } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useLocation, useNavigate } from 'react-router-dom'
import { Filter, minOppgavelisteFiltre } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/oppgaveFiltrering/filterLocalStorage'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  hentGosysOppgaver,
  hentOppgaverMedStatus,
  OppgaveDTO,
  OppgaveSaksbehandler,
  saksbehandlereIEnhetApi,
} from '~shared/api/oppgaver'
import { isSuccess } from '~shared/api/apiUtils'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  leggTilOppgavenIMinliste,
  oppdaterFrist,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { FilterRad } from '~components/oppgavebenk/oppgaveFiltrering/FilterRad'
import { VelgOppgavestatuser } from '~components/oppgavebenk/oppgaveFiltrering/VelgOppgavestatuser'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/components/OppgaveFeilWrapper'
import { hentAlleStoettedeRevurderinger } from '~shared/api/revurdering'
import { RevurderingsaarsakerBySakstype, RevurderingsaarsakerDefault } from '~shared/types/Revurderingaarsak'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')

  const location = useLocation()
  const navigate = useNavigate()

  const [oppgavelistaOppgaver, setOppgavelistaOppgaver] = useState<Array<OppgaveDTO>>([])
  const [minOppgavelisteOppgaver, setMinOppgavelisteOppgaver] = useState<Array<OppgaveDTO>>([])

  const [oppgavelistaFilter, setOppgavelistaFilter] = useState<Filter>(hentFilterFraLocalStorage())
  const [minOppgavelisteFilter, setMinOppgavelisteFilter] = useState<Filter>(minOppgavelisteFiltre())

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)
  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [hentRevurderingsaarsakerStatus, hentRevurderingsaarsaker] = useApiCall(hentAlleStoettedeRevurderinger)
  const [revurderingsaarsaker, setRevurderingsaarsaker] = useState<RevurderingsaarsakerBySakstype>(
    new RevurderingsaarsakerDefault()
  )

  const oppdaterOppgavelisteValg = (oppgaveListeValg: OppgavelisteToggle) => {
    setOppgaveListeValg(oppgaveListeValg)
    if (oppgaveListeValg === 'MinOppgaveliste') {
      navigate('/minoppgaveliste')
    } else {
      navigate('/')
    }
  }

  const hentMinOppgavelisteOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentMinOppgavelisteOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : minOppgavelisteFilter.oppgavestatusFilter,
      minOppgavelisteIdent: true,
    })

  const hentOppgavelistaOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentOppgavelistaOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : oppgavelistaFilter.oppgavestatusFilter,
      minOppgavelisteIdent: false,
    })

  const hentAlleMinOppgavelisteOppgaver = () => {
    hentMinOppgavelisteOppgaver()
    hentGosysOppgaverFetch({})
  }

  const hentAlleOppgaver = () => {
    hentMinOppgavelisteOppgaver()
    hentOppgavelistaOppgaver()
    hentGosysOppgaverFetch({})
    hentRevurderingsaarsaker({})
  }

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavelistaOppgaver, oppgave.id, saksbehandler, versjon)
      )
      if (innloggetSaksbehandler.ident === saksbehandler?.ident) {
        setMinOppgavelisteOppgaver(leggTilOppgavenIMinliste(minOppgavelisteOppgaver, oppgave, saksbehandler, versjon))
      } else {
        setMinOppgavelisteOppgaver(
          filtrerKunInnloggetBrukerOppgaver(
            finnOgOppdaterSaksbehandlerTildeling(minOppgavelisteOppgaver, oppgave.id, saksbehandler, versjon)
          )
        )
      }
    }, 2000)
  }

  useEffect(() => {
    if (location.pathname.includes('minoppgaveliste')) {
      if (oppgaveListeValg !== 'MinOppgaveliste') {
        setOppgaveListeValg('MinOppgaveliste')
      }
    }
  }, [location.pathname])

  useEffect(() => {
    leggFilterILocalStorage({ ...oppgavelistaFilter, fnrFilter: '', sakidFilter: '' })
  }, [oppgavelistaFilter])

  useEffect(() => {
    hentAlleOppgaver()
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereIEnheter(saksbehandlere)
      })
    }
  }, [])

  // Denne spaghettien er pga at vi må ha Gosys oppgaver inn i våres oppgave behandling
  useEffect(() => {
    if (isSuccess(oppgavelistaOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...oppgavelistaOppgaverResult.data,
        ...gosysOppgaverResult.data,
      ])
      setOppgavelistaOppgaver(alleOppgaverMerget)
    } else if (isSuccess(oppgavelistaOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setOppgavelistaOppgaver(sorterOppgaverEtterOpprettet(oppgavelistaOppgaverResult.data))
    } else if (!isSuccess(oppgavelistaOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setOppgavelistaOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaverResult.data))
    }
  }, [oppgavelistaOppgaverResult, gosysOppgaverResult])

  useEffect(() => {
    if (isSuccess(minOppgavelisteOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...minOppgavelisteOppgaverResult.data,
        ...filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data),
      ])
      setMinOppgavelisteOppgaver(alleOppgaverMerget)
    } else if (isSuccess(minOppgavelisteOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setMinOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(minOppgavelisteOppgaverResult.data))
    } else if (!isSuccess(minOppgavelisteOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setMinOppgavelisteOppgaver(
        sorterOppgaverEtterOpprettet(filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data))
      )
    }
  }, [gosysOppgaverResult, minOppgavelisteOppgaverResult])

  useEffect(() => {
    if (isSuccess(hentRevurderingsaarsakerStatus)) {
      setRevurderingsaarsaker(hentRevurderingsaarsakerStatus.data)
    }
  }, [hentRevurderingsaarsakerStatus])

  return (
    <Container>
      <TabsWidth
        value={oppgaveListeValg}
        onChange={(e) => {
          oppdaterOppgavelisteValg(e as OppgavelisteToggle)
        }}
      >
        <Tabs.List>
          <Tabs.Tab
            value="Oppgavelista"
            label={`Oppgavelisten (${oppgavelistaOppgaver.length})`}
            icon={<InboxIcon />}
          />
          <Tabs.Tab
            value="MinOppgaveliste"
            label={`Min oppgaveliste (${minOppgavelisteOppgaver.length})`}
            icon={<PersonIcon aria-hidden />}
          />
        </Tabs.List>
      </TabsWidth>

      {oppgaveListeValg === 'MinOppgaveliste' ? (
        <>
          <FlexRow>
            <VelgOppgavestatuser
              value={minOppgavelisteFilter.oppgavestatusFilter}
              onChange={(oppgavestatusFilter) => {
                hentMinOppgavelisteOppgaver(oppgavestatusFilter)
                setMinOppgavelisteFilter({ ...minOppgavelisteFilter, oppgavestatusFilter })
              }}
            />
          </FlexRow>

          <OppgaveFeilWrapper oppgaver={minOppgavelisteOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
            <Oppgaver
              oppgaver={minOppgavelisteOppgaver}
              oppdaterTildeling={oppdaterSaksbehandlerTildeling}
              oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
                oppdaterFrist(setMinOppgavelisteOppgaver, minOppgavelisteOppgaver, id, nyfrist, versjon)
              }
              saksbehandlereIEnhet={saksbehandlereIEnheter}
              revurderingsaarsaker={revurderingsaarsaker}
            />
          </OppgaveFeilWrapper>
        </>
      ) : (
        <>
          <FilterRad
            hentAlleOppgaver={hentAlleMinOppgavelisteOppgaver}
            hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => hentOppgavelistaOppgaver(oppgavestatusFilter)}
            filter={oppgavelistaFilter}
            setFilter={setOppgavelistaFilter}
            alleOppgaver={oppgavelistaOppgaver}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
          />

          <OppgaveFeilWrapper oppgaver={minOppgavelisteOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
            <Oppgaver
              oppgaver={oppgavelistaOppgaver}
              oppdaterTildeling={oppdaterSaksbehandlerTildeling}
              saksbehandlereIEnhet={saksbehandlereIEnheter}
              filter={oppgavelistaFilter}
              revurderingsaarsaker={revurderingsaarsaker}
            />
          </OppgaveFeilWrapper>
        </>
      )}
    </Container>
  )
}

const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
