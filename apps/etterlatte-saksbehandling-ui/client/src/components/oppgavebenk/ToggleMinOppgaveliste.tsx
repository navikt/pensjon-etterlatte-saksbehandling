import React, { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useLocation, useNavigate } from 'react-router-dom'
import { Filter, minOppgavelisteFiltre } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/oppgaveFiltrering/filterLocalStorage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaverMedStatus, OppgaveDTO, saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { isSuccess } from '~shared/api/apiUtils'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  leggTilOppgavenIMinliste,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { MinOppgaveliste } from '~components/oppgavebenk/MinOppgaveliste'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { Saksbehandler } from '~shared/types/saksbehandler'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [hovedsideOppgaver, setHovedsideOppgaver] = useState<Array<OppgaveDTO>>([])
  const [minsideOppgaver, setMinsideOppgaver] = useState<Array<OppgaveDTO>>([])

  const [minsideFilter, setMinsideFilter] = useState<Filter>(minOppgavelisteFiltre())
  const [hovedsideFilter, setHovedsideFilter] = useState<Filter>(hentFilterFraLocalStorage())

  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')

  const [, hentSaksbehandlereIEnhet] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereForEnhet, setSaksbehandlereForEnhet] = useState<Array<Saksbehandler>>([])

  const [minsideOppgaverResult, hentOppgaverMinside] = useApiCall(hentOppgaverMedStatus)
  const [hovedsideOppgaverResult, hentAlleOppgaverStatusFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaverResult, hentGosysOppgaverFunc] = useApiCall(hentGosysOppgaver)

  const location = useLocation()
  const navigate = useNavigate()

  const oppdaterOppgavelisteValg = (oppgaveListeValg: OppgavelisteToggle) => {
    setOppgaveListeValg(oppgaveListeValg)
    if (oppgaveListeValg === 'MinOppgaveliste') {
      navigate('/minoppgaveliste')
    } else {
      navigate('/')
    }
  }

  const hentMinsideOppgaver = (oppgavestatusFilter: Array<string> | undefined) =>
    hentOppgaverMinside({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : minsideFilter.oppgavestatusFilter,
      minOppgavelisteIdent: true,
    })

  const hentHovedsideOppgaver = (oppgavestatusFilter: Array<string> | undefined) =>
    hentAlleOppgaverStatusFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : hovedsideFilter.oppgavestatusFilter,
      minOppgavelisteIdent: false,
    })

  const hentHovedsideOppgaverAlle = () => {
    hentMinsideOppgaver(undefined)
    hentGosysOppgaverFunc({})
  }

  const hentAlleOppgaver = () => {
    hentMinsideOppgaver(undefined)
    hentHovedsideOppgaver(undefined)
    hentGosysOppgaverFunc({})
  }

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident)
  }

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: string | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      setHovedsideOppgaver(finnOgOppdaterSaksbehandlerTildeling(hovedsideOppgaver, oppgave.id, saksbehandler, versjon))
      if (innloggetSaksbehandler.ident === saksbehandler) {
        setMinsideOppgaver(leggTilOppgavenIMinliste(minsideOppgaver, oppgave, saksbehandler, versjon))
      } else {
        setMinsideOppgaver(
          filtrerKunInnloggetBrukerOppgaver(
            finnOgOppdaterSaksbehandlerTildeling(minsideOppgaver, oppgave.id, saksbehandler, versjon)
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
    leggFilterILocalStorage(hovedsideFilter)
  }, [hovedsideFilter])

  useEffect(() => {
    hentAlleOppgaver()
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnhet({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereForEnhet(saksbehandlere)
      })
    }
  }, [])

  useEffect(() => {
    if (isSuccess(hovedsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...hovedsideOppgaverResult.data,
        ...gosysOppgaverResult.data,
      ])
      setHovedsideOppgaver(alleOppgaverMerget)
    } else if (isSuccess(hovedsideOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setHovedsideOppgaver(sorterOppgaverEtterOpprettet(hovedsideOppgaverResult.data))
    } else if (!isSuccess(hovedsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setHovedsideOppgaver(sorterOppgaverEtterOpprettet(gosysOppgaverResult.data))
    }
  }, [hovedsideOppgaverResult, gosysOppgaverResult])

  useEffect(() => {
    if (isSuccess(minsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      const alleOppgaverMerget = sorterOppgaverEtterOpprettet([
        ...minsideOppgaverResult.data,
        ...filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data),
      ])
      setMinsideOppgaver(alleOppgaverMerget)
    } else if (isSuccess(minsideOppgaverResult) && !isSuccess(gosysOppgaverResult)) {
      setMinsideOppgaver(sorterOppgaverEtterOpprettet(minsideOppgaverResult.data))
    } else if (!isSuccess(minsideOppgaverResult) && isSuccess(gosysOppgaverResult)) {
      setMinsideOppgaver(sorterOppgaverEtterOpprettet(filtrerKunInnloggetBrukerOppgaver(gosysOppgaverResult.data)))
    }
  }, [gosysOppgaverResult, minsideOppgaverResult])

  return (
    <Container>
      <TabsWidth
        value={oppgaveListeValg}
        onChange={(e) => {
          oppdaterOppgavelisteValg(e as OppgavelisteToggle)
        }}
      >
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label={`Oppgavelisten (${hovedsideOppgaver.length})`} icon={<InboxIcon />} />
          <Tabs.Tab
            value="MinOppgaveliste"
            label={`Min oppgaveliste (${minsideOppgaver.length})`}
            icon={<PersonIcon aria-hidden />}
          />
        </Tabs.List>
      </TabsWidth>
      {oppgaveListeValg === 'MinOppgaveliste' ? (
        <MinOppgaveliste
          minsideOppgaver={minsideOppgaver}
          minsideOppgaverResult={minsideOppgaverResult}
          gosysOppgaverResult={gosysOppgaverResult}
          minsideFilter={minsideFilter}
          setMinsideFilter={(filter: Filter) => {
            hentMinsideOppgaver(filter.oppgavestatusFilter)
            setMinsideFilter(filter)
          }}
          setMinsideOppgaver={setMinsideOppgaver}
          saksbehandlereIEnhet={saksbehandlereForEnhet}
          oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
        />
      ) : (
        <Oppgavelista
          saksbehandlereIEnhet={saksbehandlereForEnhet}
          hovedsideOppgaver={hovedsideOppgaver}
          hentHovedsideOppgaverAlle={hentHovedsideOppgaverAlle}
          hovedsideOppgaverResult={hovedsideOppgaverResult}
          gosysOppgaverResult={gosysOppgaverResult}
          hentHovedsideOppgaver={hentHovedsideOppgaver}
          hovedsideFilter={hovedsideFilter}
          setHovedsideFilter={setHovedsideFilter}
          oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
        />
      )}
    </Container>
  )
}

const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
