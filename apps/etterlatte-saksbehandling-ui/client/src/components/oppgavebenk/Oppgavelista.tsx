import React, { useEffect, useState } from 'react'
import { SortState } from '@navikt/ds-react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { soekOppgaver } from '~shared/api/oppgaver'
import {
  finnOgOppdaterOppgave,
  hentPagineringSizeFraLocalStorage,
} from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { Saksbehandler } from '~shared/types/saksbehandler'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import {
  hentOppgavelistenFilterFraLocalStorage,
  leggOppgavelistenFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { byggOppgaveSoekRequest } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
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
  const [filter, setFilter] = useState<Filter>(hentOppgavelistenFilterFraLocalStorage())
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
    const newSort: SortState =
      sort && sortKey === sort.orderBy && sort.direction === 'descending'
        ? { orderBy: sortKey, direction: 'none' }
        : {
            orderBy: sortKey,
            direction: sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
          }
    setSort(newSort)

    let nySortering: OppgaveSortering = { ...initialSortering }
    switch (sortKey as SortKey) {
      case SortKey.REGISTRERINGSDATO:
        nySortering = { ...initialSortering, registreringsdatoSortering: newSort.direction }
        break
      case SortKey.FRIST:
        nySortering = { ...initialSortering, fristSortering: newSort.direction }
        break
      case SortKey.FNR:
        nySortering = { ...initialSortering, fnrSortering: newSort.direction }
        break
    }
    setSortering(nySortering)
    leggTilSorteringILocalStorage(nySortering)
  }

  useEffect(() => {
    leggOppgavelistenFilterILocalStorage({ ...filter, sakEllerFnrFilter: '' })
  }, [filter])

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

      {oppgaver.length >= 0 && !isPending(soekResult) ? (
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
        />
      ) : (
        mapResult(soekResult, {
          pending: <Spinner label="Henter oppgaver" />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>,
        })
      )}
    </>
  )
}
