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
  leggTilOppgavenIMinliste,
  oppdaterFrist,
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
import { minOppgavelisteFiltre } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { GosysOppgaveliste } from '~components/oppgavebenk/GosysOppgaveliste'

export const Oppgavelista = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgavelisteValg, setOppgavelisteValg] = useState<OppgavelisteValg>(
    hentValgFraLocalStorage() as OppgavelisteValg
  )

  const [oppgavelistaOppgaver, setOppgavelistaOppgaver] = useState<Array<OppgaveDTO>>([])
  const [minOppgavelisteOppgaver, setMinOppgavelisteOppgaver] = useState<Array<OppgaveDTO>>([])

  const [oppgavelistaFilter, setOppgavelistaFilter] = useState<Filter>(hentFilterFraLocalStorage())
  const [minOppgavelisteFilter, setMinOppgavelisteFilter] = useState<Filter>(minOppgavelisteFiltre())

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)
  const [minOppgavelisteOppgaverResult, hentMinOppgavelisteOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const [, hentSaksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])

  const [hentRevurderingsaarsakerStatus, hentRevurderingsaarsaker] = useApiCall(hentAlleStoettedeRevurderinger)
  const [revurderingsaarsaker, setRevurderingsaarsaker] = useState<RevurderingsaarsakerBySakstype>(
    new RevurderingsaarsakerDefault()
  )

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

  const hentMineOppgaver = () => {
    hentMinOppgavelisteOppgaver()
  }

  const hentAlleOppgaver = () => {
    hentMinOppgavelisteOppgaver()
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
    if (isSuccess(minOppgavelisteOppgaverResult)) {
      setMinOppgavelisteOppgaver(sorterOppgaverEtterOpprettet(minOppgavelisteOppgaverResult.data))
    }
  }, [minOppgavelisteOppgaverResult])

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
        antallMinOppgavelisteOppgaver={minOppgavelisteOppgaver.length}
      />
      {oppgavelisteValg === OppgavelisteValg.MIN_OPPGAVELISTE && (
        <>
          <FilterRad
            key={OppgavelisteValg.MIN_OPPGAVELISTE}
            hentAlleOppgaver={hentMineOppgaver}
            hentOppgaverStatus={(oppgavestatusFilter: Array<string>) =>
              hentMinOppgavelisteOppgaver(oppgavestatusFilter)
            }
            filter={minOppgavelisteFilter}
            setFilter={setMinOppgavelisteFilter}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
          />
          <OppgaveFeilWrapper oppgaver={minOppgavelisteOppgaverResult}>
            <Oppgaver
              oppgaver={minOppgavelisteOppgaver}
              oppdaterTildeling={oppdaterSaksbehandlerTildeling}
              oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
                oppdaterFrist(setMinOppgavelisteOppgaver, minOppgavelisteOppgaver, id, nyfrist, versjon)
              }
              filter={minOppgavelisteFilter}
              saksbehandlereIEnhet={saksbehandlereIEnheter}
              revurderingsaarsaker={revurderingsaarsaker}
            />
          </OppgaveFeilWrapper>
        </>
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
