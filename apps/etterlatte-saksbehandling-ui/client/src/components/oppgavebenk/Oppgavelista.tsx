import React, { useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
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
  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

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

  const hentMineOgGosysOppgaver = () => {
    hentMinOppgavelisteOppgaver()
    hentGosysOppgaverFetch({})
  }

  const hentAlleOppgaver = () => {
    hentMinOppgavelisteOppgaver()
    hentOppgavelistaOppgaver()
    hentGosysOppgaverFetch({})
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
      {oppgavelisteValg === OppgavelisteValg.MIN_OPPGAVELISTE ? (
        <>
          <FilterRad
            key={OppgavelisteValg.MIN_OPPGAVELISTE}
            hentAlleOppgaver={hentMineOgGosysOppgaver}
            hentOppgaverStatus={(oppgavestatusFilter: Array<string>) =>
              hentMinOppgavelisteOppgaver(oppgavestatusFilter)
            }
            filter={minOppgavelisteFilter}
            setFilter={setMinOppgavelisteFilter}
            saksbehandlereIEnhet={saksbehandlereIEnheter}
          />
          <OppgaveFeilWrapper oppgaver={minOppgavelisteOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
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
      ) : (
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
          <OppgaveFeilWrapper oppgaver={oppgavelistaOppgaverResult} gosysOppgaver={gosysOppgaverResult}>
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
