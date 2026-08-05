import React, { useEffect, useState } from 'react'
import { SortState } from '@navikt/ds-react'
import { isPendingOrInitial, mapResult } from '~shared/api/apiUtils'
import { soekOppgaver } from '~shared/api/oppgaver'
import {
  finnOgOppdaterOppgave,
  hentPagineringSizeFraLocalStorage,
} from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import {
  hentOppgavelistenFilterFraLocalStorage,
  leggOppgavelistenFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { byggOppgaveSoekRequest } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { OPPGAVELISTA_URL_NOKLER, useFilterMedUrl } from '~components/oppgavebenk/filtreringAvOppgaver/useFilterMedUrl'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import {
  initialSortering,
  leggTilSorteringILocalStorage,
  OppgaveSortering,
  hentSorteringFraLocalStorage,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { SortKey } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const Oppgavelista = ({ saksbehandlereIEnhet }: Props) => {
  const [filter, setFilter] = useFilterMedUrl(
    OPPGAVELISTA_URL_NOKLER,
    hentOppgavelistenFilterFraLocalStorage,
    (f: Filter) => leggOppgavelistenFilterILocalStorage({ ...f, sakEllerFnrFilter: '' })
  )

  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())
  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())
  const [sort, setSort] = useState<SortState | undefined>(undefined)
  const [oppgaver, setOppgaver] = useState<OppgaveDTO[]>([])
  const [totaltAntall, setTotaltAntall] = useState<number>(0)

  const dispatcher = useOppgavebenkStateDispatcher()
  const [soekResult, soekFetch] = useApiCall(soekOppgaver)

  const hentOppgaver = (nyFilter?: Filter, nyPage?: number) => {
    const aktivFilter = nyFilter ?? filter
    const aktivPage = nyPage ?? page
    soekFetch(byggOppgaveSoekRequest(aktivFilter, sortering, aktivPage - 1, rowsPerPage, false), (result) => {
      setOppgaver(result.oppgaver)
      setTotaltAntall(result.totaltAntall)
      dispatcher.refreshStats()
    })
  }

  const handleSort = (sortKey: string) => {
    const nySortering: SortState =
      sort && sortKey === sort.orderBy && sort.direction === 'descending'
        ? { orderBy: sortKey, direction: 'none' }
        : {
            orderBy: sortKey,
            direction: sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
          }
    setSort(nySortering)

    let nyOppgaveSortering: OppgaveSortering = { ...initialSortering }
    switch (sortKey as SortKey) {
      case SortKey.REGISTRERINGSDATO:
        nyOppgaveSortering = { ...initialSortering, registreringsdatoSortering: nySortering.direction }
        break
      case SortKey.FRIST:
        nyOppgaveSortering = { ...initialSortering, fristSortering: nySortering.direction }
        break
      case SortKey.FNR:
        nyOppgaveSortering = { ...initialSortering, fnrSortering: nySortering.direction }
        break
    }
    setSortering(nyOppgaveSortering)
    leggTilSorteringILocalStorage(nyOppgaveSortering)
  }

  useEffect(() => {
    hentOppgaver()
  }, [filter, page, rowsPerPage, sortering])

  const oppdaterSaksbehandlerTildeling = (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => {
    setTimeout(() => {
      setOppgaver((prev) =>
        finnOgOppdaterOppgave(prev, oppgave.id, {
          status: Oppgavestatus.UNDER_BEHANDLING,
          saksbehandler,
        })
      )
    }, 2000)
  }

  const oppdaterStatus = (oppgaveId: string, status: Oppgavestatus) => {
    setTimeout(() => {
      setOppgaver((prev) => finnOgOppdaterOppgave(prev, oppgaveId, { status }))
    }, 2000)
  }

  const oppdaterFrist = (oppgaveId: string, frist: string) => {
    setTimeout(() => {
      setOppgaver((prev) => finnOgOppdaterOppgave(prev, oppgaveId, { frist }))
    }, 2000)
  }

  const oppdaterMerknad = (oppgaveId: string, merknad: string) => {
    setTimeout(() => {
      setOppgaver((prev) => finnOgOppdaterOppgave(prev, oppgaveId, { merknad }))
    }, 2000)
  }

  return (
    <>
      <FilterRad
        hentAlleOppgaver={(oppgavestatusFilter) => {
          const nyFilter = oppgavestatusFilter !== undefined ? { ...filter, oppgavestatusFilter } : filter
          setPage(1)
          hentOppgaver(nyFilter, 1)
        }}
        filter={filter}
        setFilter={(nyFilter) => {
          setFilter(nyFilter)
          setPage(1)
        }}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppgavelisteValg={OppgavelisteValg.OPPGAVELISTA}
      />

      {mapResult(soekResult, {
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>,
      })}

      <Oppgaver
        oppgaver={oppgaver}
        totaltAntall={totaltAntall}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
        oppdaterStatus={oppdaterStatus}
        oppdaterFrist={oppdaterFrist}
        oppdaterMerknad={oppdaterMerknad}
        page={page}
        setPage={setPage}
        rowsPerPage={rowsPerPage}
        setRowsPerPage={setRowsPerPage}
        sort={sort}
        handleSort={handleSort}
        isLoading={isPendingOrInitial(soekResult)}
      />
    </>
  )
}
