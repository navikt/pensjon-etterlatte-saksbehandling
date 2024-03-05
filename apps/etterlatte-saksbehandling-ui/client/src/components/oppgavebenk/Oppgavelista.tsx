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
} from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { oppgavelisteValg, VelgOppgaveliste } from '~components/oppgavebenk/velgOppgaveliste/VelgOppgaveliste'

export const Oppgavelista = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgaveListeValg, setOppgaveListeValg] = useState<oppgavelisteValg>(
    hentValgFraLocalStorage() as oppgavelisteValg
  )

  const [oppgavelistaOppgaver, setOppgavelistaOppgaver] = useState<Array<OppgaveDTO>>([])
  const [minOppgavelisteOppgaver, setMinOppgavelisteOppgaver] = useState<Array<OppgaveDTO>>([])

  const [oppgavelistaFilter, setOppgavelistaFilter] = useState<Filter>(hentFilterFraLocalStorage())

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
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : oppgavelistaFilter.oppgavestatusFilter,
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
    leggValgILocalstorage(oppgaveListeValg)
  }, [oppgaveListeValg])

  return (
    <Container>
      <VelgOppgaveliste
        oppgavelisteValg={oppgaveListeValg}
        setOppgavelisteValg={setOppgaveListeValg}
        antallOppgavelistaOppgaver={oppgavelistaOppgaver.length}
        antallMinOppgavelisteOppgaver={minOppgavelisteOppgaver.length}
      />

      <FilterRad
        hentAlleOppgaver={oppgaveListeValg === 'MinOppgaveliste' ? hentMineOgGosysOppgaver : hentAlleOppgaver}
        hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => {
          oppgaveListeValg === 'MinOppgaveliste'
            ? hentMinOppgavelisteOppgaver(oppgavestatusFilter)
            : hentOppgavelistaOppgaver(oppgavestatusFilter)
        }}
        filter={oppgavelistaFilter}
        setFilter={setOppgavelistaFilter}
        saksbehandlereIEnhet={saksbehandlereIEnheter}
      />

      <OppgaveFeilWrapper
        oppgaver={oppgaveListeValg === 'MinOppgaveliste' ? minOppgavelisteOppgaverResult : oppgavelistaOppgaverResult}
        gosysOppgaver={gosysOppgaverResult}
      >
        <Oppgaver
          oppgaver={oppgaveListeValg === 'MinOppgaveliste' ? minOppgavelisteOppgaver : oppgavelistaOppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
            oppgaveListeValg === 'MinOppgaveliste' &&
            oppdaterFrist(setMinOppgavelisteOppgaver, minOppgavelisteOppgaver, id, nyfrist, versjon)
          }
          saksbehandlereIEnhet={saksbehandlereIEnheter}
          filter={oppgavelistaFilter}
          revurderingsaarsaker={revurderingsaarsaker}
        />
      </OppgaveFeilWrapper>
    </Container>
  )
}
