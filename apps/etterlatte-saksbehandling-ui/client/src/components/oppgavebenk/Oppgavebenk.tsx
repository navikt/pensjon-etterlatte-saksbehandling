import React, { ReactNode, useEffect, useState } from 'react'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import {
  hentValgFraLocalStorage,
  leggValgILocalstorage,
  OppgavelisteValg,
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

  const [oppgavelisteValg, setOppgavelisteValg] = useState<OppgavelisteValg>(
    hentValgFraLocalStorage() as OppgavelisteValg
  )

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereIEnheter(saksbehandlere)
      })
    }
  }, [])

  useEffect(() => {
    leggValgILocalstorage(oppgavelisteValg)
  }, [oppgavelisteValg])

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
      <Box padding="space-8">
        <VelgOppgaveliste oppgavelisteValg={oppgavelisteValg} setOppgavelisteValg={setOppgavelisteValg} />
        {rendreValgtOppgaveliste()}
      </Box>
    </ProvideOppgavebenkContext>
  )
}
