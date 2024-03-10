import React, { ReactNode, useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, hentOppgaverMedStatus, saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { isSuccess } from '~shared/api/apiUtils'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { hentAlleStoettedeRevurderinger } from '~shared/api/revurdering'
import { RevurderingsaarsakerBySakstype, RevurderingsaarsakerDefault } from '~shared/types/Revurderingaarsak'
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

export const Oppgavelistene = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgavelisteValg, setOppgavelisteValg] = useState<OppgavelisteValg>(
    hentValgFraLocalStorage() as OppgavelisteValg
  )

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)
  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)
  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [hentRevurderingsaarsakerStatus, hentRevurderingsaarsaker] = useApiCall(hentAlleStoettedeRevurderinger)
  const [revurderingsaarsaker, setRevurderingsaarsaker] = useState<RevurderingsaarsakerBySakstype>(
    new RevurderingsaarsakerDefault()
  )

  useEffect(() => {
    hentRevurderingsaarsaker({})
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereIEnheter(saksbehandlere)
      })
    }
  }, [])

  useEffect(() => {
    if (isSuccess(hentRevurderingsaarsakerStatus)) {
      setRevurderingsaarsaker(hentRevurderingsaarsakerStatus.data)
    }
  }, [hentRevurderingsaarsakerStatus])

  useEffect(() => {
    leggValgILocalstorage(oppgavelisteValg)
  }, [oppgavelisteValg])

  const rendreValgtOppgaveliste = (): ReactNode => {
    switch (oppgavelisteValg) {
      case OppgavelisteValg.OPPGAVELISTA:
        return (
          <Oppgavelista
            key={OppgavelisteValg.OPPGAVELISTA}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
            revurderingsaarsaker={revurderingsaarsaker}
            oppgavelistaOppgaverResult={oppgavelistaOppgaverResult}
            hentOppgavelistaOppgaverFetch={hentOppgavelistaOppgaverFetch}
          />
        )
      case OppgavelisteValg.MIN_OPPGAVELISTE:
        return (
          <MinOppgaveliste
            key={OppgavelisteValg.MIN_OPPGAVELISTE}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
            revurderingsaarsaker={revurderingsaarsaker}
            minOppgavelisteOppgaverResult={minOppgavelisteOppgaverResult}
            hentMinOppgavelisteOppgaverFetch={hentMinOppgavelisteOppgaverFetch}
          />
        )
      case OppgavelisteValg.GOSYS_OPPGAVER:
        return (
          <GosysOppgaveliste
            key={OppgavelisteValg.GOSYS_OPPGAVER}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
            gosysOppgaverResult={gosysOppgaverResult}
            hentGosysOppgaverFetch={hentGosysOppgaverFetch}
          />
        )
    }
  }

  return (
    <ProvideOppgavebenkContext>
      <Container>
        <VelgOppgaveliste oppgavelisteValg={oppgavelisteValg} setOppgavelisteValg={setOppgavelisteValg} />
        {rendreValgtOppgaveliste()}
      </Container>
    </ProvideOppgavebenkContext>
  )
}
