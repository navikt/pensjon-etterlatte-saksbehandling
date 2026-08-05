import React, { ReactNode, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import {
  hentValgFraLocalStorage,
  leggValgILocalstorage,
  NUMMER_TIL_TAB,
  OppgavelisteValg,
  TAB_NUMMER,
} from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { VelgOppgaveliste } from '~components/oppgavebenk/velgOppgaveliste/VelgOppgaveliste'
import { GosysOppgaveliste } from '~components/oppgavebenk/GosysOppgaveliste'
import { MinOppgaveliste } from '~components/oppgavebenk/MinOppgaveliste'
import { Oppgavelista } from '~components/oppgavebenk/Oppgavelista'
import { ProvideOppgavebenkContext } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Box } from '@navikt/ds-react'

export const Oppgavebenk = () => {
  useSidetittel('Oppgavebenk')

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  if (!innloggetSaksbehandler.skriveEnheter.length) {
    return <Tilgangsmelding />
  }

  const [searchParams, setSearchParams] = useSearchParams()

  const [oppgavelisteValg, setOppgavelisteValg] = useState<OppgavelisteValg>(() => {
    const tabFraUrl = searchParams.get('tab')
    if (tabFraUrl && NUMMER_TIL_TAB[tabFraUrl]) return NUMMER_TIL_TAB[tabFraUrl]
    return hentValgFraLocalStorage() as OppgavelisteValg
  })

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereIEnheter(saksbehandlere)
      })
    }
  }, [])

  // Sett tab i URL ved mount hvis den mangler.
  // Vi leser window.location.search direkte fordi barnekomponenter (useFilterMedUrl) kan ha
  // satt filter-params via history.replaceState før denne effekten kjører, men React Routers
  // interne state (prev) er ikke oppdatert ennå og er stale. Leser vi window.location.search
  // her bevares filter-params som barnet satte.
  useEffect(() => {
    const faktiskeParams = new URLSearchParams(window.location.search)
    if (!faktiskeParams.get('tab')) {
      faktiskeParams.set('tab', TAB_NUMMER[oppgavelisteValg])
      setSearchParams(faktiskeParams, { replace: true })
    }
  }, [])

  // Byttter tab: oppdaterer URL (fjerner filter-params), localStorage og state
  const byttTab = (nyValg: OppgavelisteValg) => {
    setOppgavelisteValg(nyValg)
    leggValgILocalstorage(nyValg)
    // Setter kun tab i URL – filtere fra forrige tab fjernes bevisst.
    // Det nye tabets filter-params settes av useFilterMedUrl ved mount.
    setSearchParams({ tab: TAB_NUMMER[nyValg] }, { replace: true })
  }

  const rendreValgtOppgaveliste = (): ReactNode => {
    switch (oppgavelisteValg) {
      case OppgavelisteValg.OPPGAVELISTA:
        return <Oppgavelista key={OppgavelisteValg.OPPGAVELISTA} saksbehandlereIEnhet={saksbehandlereIEnheter} />
      case OppgavelisteValg.MIN_OPPGAVELISTE:
        return <MinOppgaveliste key={OppgavelisteValg.MIN_OPPGAVELISTE} saksbehandlereIEnhet={saksbehandlereIEnheter} />
      case OppgavelisteValg.GOSYS_OPPGAVER:
        return <GosysOppgaveliste key={OppgavelisteValg.GOSYS_OPPGAVER} saksbehandlereIEnhet={saksbehandlereIEnheter} />
    }
  }

  return (
    <ProvideOppgavebenkContext>
      <Box padding="space-32">
        <VelgOppgaveliste oppgavelisteValg={oppgavelisteValg} setOppgavelisteValg={byttTab} />
        {rendreValgtOppgaveliste()}
      </Box>
    </ProvideOppgavebenkContext>
  )
}
