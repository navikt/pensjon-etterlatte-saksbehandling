import React, { useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedStatus, OppgaveDTO, OppgaveSaksbehandler, saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { isSuccess } from '~shared/api/apiUtils'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { OppgaveFeilWrapper } from '~components/oppgavebenk/components/OppgaveFeilWrapper'
import { hentAlleStoettedeRevurderinger } from '~shared/api/revurdering'
import { RevurderingsaarsakerBySakstype, RevurderingsaarsakerDefault } from '~shared/types/Revurderingaarsak'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import {
  hentValgFraLocalStorage,
  leggValgILocalstorage,
  OppgavelisteValg,
} from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { VelgOppgaveliste } from '~components/oppgavebenk/velgOppgaveliste/VelgOppgaveliste'
import { GosysOppgaveliste } from '~components/oppgavebenk/GosysOppgaveliste'
import { MinOppgaveliste } from '~components/oppgavebenk/MinOppgaveliste'

export const Oppgavelistene = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgavelisteValg, setOppgavelisteValg] = useState<OppgavelisteValg>(
    hentValgFraLocalStorage() as OppgavelisteValg
  )

  const [oppgavelistaOppgaver, setOppgavelistaOppgaver] = useState<Array<OppgaveDTO>>([])

  const [oppgavelistaFilter, setOppgavelistaFilter] = useState<Filter>(hentFilterFraLocalStorage())

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [hentRevurderingsaarsakerStatus, hentRevurderingsaarsaker] = useApiCall(hentAlleStoettedeRevurderinger)
  const [revurderingsaarsaker, setRevurderingsaarsaker] = useState<RevurderingsaarsakerBySakstype>(
    new RevurderingsaarsakerDefault()
  )

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      setOppgavelistaOppgaver(
        finnOgOppdaterSaksbehandlerTildeling(oppgavelistaOppgaver, oppgave.id, saksbehandler, versjon)
      )
    }, 2000)
  }

  const hentOppgavelistaOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentOppgavelistaOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : oppgavelistaFilter.oppgavestatusFilter,
      minOppgavelisteIdent: false,
    })

  const hentAlleOppgaver = () => {
    hentOppgavelistaOppgaver()
    hentRevurderingsaarsaker({})
  }

  useEffect(() => {
    hentAlleOppgaver()
    if (!!innloggetSaksbehandler.enheter.length) {
      hentSaksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, (saksbehandlere) => {
        setSaksbehandlereIEnheter(saksbehandlere)
      })
    }
  }, [])

  useEffect(() => {
    if (isSuccess(oppgavelistaOppgaverResult)) {
      setOppgavelistaOppgaver(sorterOppgaverEtterOpprettet(oppgavelistaOppgaverResult.data))
    }
  }, [oppgavelistaOppgaverResult])

  useEffect(() => {
    if (isSuccess(hentRevurderingsaarsakerStatus)) {
      setRevurderingsaarsaker(hentRevurderingsaarsakerStatus.data)
    }
  }, [hentRevurderingsaarsakerStatus])

  useEffect(() => {
    leggFilterILocalStorage({ ...oppgavelistaFilter, sakEllerFnrFilter: '' })
  }, [oppgavelistaFilter])

  useEffect(() => {
    leggValgILocalstorage(oppgavelisteValg)
  }, [oppgavelisteValg])

  return (
    <Container>
      <VelgOppgaveliste
        oppgavelisteValg={oppgavelisteValg}
        setOppgavelisteValg={setOppgavelisteValg}
        antallOppgavelistaOppgaver={oppgavelistaOppgaver.length}
        antallMinOppgavelisteOppgaver={0}
      />
      {oppgavelisteValg === OppgavelisteValg.MIN_OPPGAVELISTE && (
        <MinOppgaveliste saksbehandlereIEnhet={saksbehandlereIEnheter} revurderingsaarsaker={revurderingsaarsaker} />
      )}
      {oppgavelisteValg === OppgavelisteValg.OPPGAVELISTA && (
        <>
          <FilterRad
            key={OppgavelisteValg.OPPGAVELISTA}
            hentAlleOppgaver={hentAlleOppgaver}
            hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => {
              hentOppgavelistaOppgaver(oppgavestatusFilter)
            }}
            filter={oppgavelistaFilter}
            setFilter={setOppgavelistaFilter}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
            oppgavelisteValg={oppgavelisteValg}
          />
          <OppgaveFeilWrapper oppgaver={oppgavelistaOppgaverResult}>
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
      {oppgavelisteValg === OppgavelisteValg.GOSYS_OPPGAVER && (
        <GosysOppgaveliste
          key={OppgavelisteValg.GOSYS_OPPGAVER}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          saksbehandlereIEnhet={saksbehandlereIEnheter}
        />
      )}
    </Container>
  )
}
