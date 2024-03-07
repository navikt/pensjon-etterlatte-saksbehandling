import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedStatus, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import {
  finnOgOppdaterSaksbehandlerTildeling,
  OppgavelisteneStats,
  sorterOppgaverEtterOpprettet,
} from '~components/oppgavebenk/utils/oppgaveutils'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import {
  hentFilterFraLocalStorage,
  leggFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
  oppgavelisteneStats: OppgavelisteneStats
  setOppgavelisteneStats: Dispatch<SetStateAction<OppgavelisteneStats>>
}

export const Oppgavelista = ({
  saksbehandlereIEnhet,
  revurderingsaarsaker,
  oppgavelisteneStats,
  setOppgavelisteneStats,
}: Props) => {
  const [filter, setFilter] = useState<Filter>(hentFilterFraLocalStorage())

  const [oppgaver, setOppgaver] = useState<Array<OppgaveDTO>>([])

  const [oppgavelistaOppgaverResult, hentOppgavelistaOppgaverFetch] = useApiCall(hentOppgaverMedStatus)

  const oppdaterSaksbehandlerTildeling = (
    oppgave: OppgaveDTO,
    saksbehandler: OppgaveSaksbehandler | null,
    versjon: number | null
  ) => {
    setTimeout(() => {
      setOppgaver(finnOgOppdaterSaksbehandlerTildeling(oppgaver, oppgave.id, saksbehandler, versjon))
    }, 2000)
  }

  const hentOppgavelistaOppgaver = (oppgavestatusFilter?: Array<string>) =>
    hentOppgavelistaOppgaverFetch({
      oppgavestatusFilter: oppgavestatusFilter ? oppgavestatusFilter : filter.oppgavestatusFilter,
      minOppgavelisteIdent: false,
    })

  useEffect(() => {
    if (isSuccess(oppgavelistaOppgaverResult)) {
      setOppgaver(sorterOppgaverEtterOpprettet(oppgavelistaOppgaverResult.data))
      setOppgavelisteneStats({
        ...oppgavelisteneStats,
        antallOppgavelistaOppgaver: oppgavelistaOppgaverResult.data.length,
      })
    }
  }, [oppgavelistaOppgaverResult])

  useEffect(() => {
    hentOppgavelistaOppgaver()
  }, [])

  useEffect(() => {
    leggFilterILocalStorage({ ...filter, sakEllerFnrFilter: '' })
  }, [filter])

  return mapResult(oppgavelistaOppgaverResult, {
    pending: <Spinner visible={true} label="Henter oppgaver" />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>,
    success: () => (
      <>
        <FilterRad
          hentAlleOppgaver={hentOppgavelistaOppgaver}
          hentOppgaverStatus={(oppgavestatusFilter: Array<string>) => {
            hentOppgavelistaOppgaver(oppgavestatusFilter)
          }}
          filter={filter}
          setFilter={setFilter}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppgavelisteValg={OppgavelisteValg.OPPGAVELISTA}
        />
        <Oppgaver
          oppgaver={oppgaver}
          oppdaterTildeling={oppdaterSaksbehandlerTildeling}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          revurderingsaarsaker={revurderingsaarsaker}
        />
      </>
    ),
  })
}
