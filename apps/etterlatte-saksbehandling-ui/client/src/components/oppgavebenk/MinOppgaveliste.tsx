import React, { useEffect, useState } from 'react'
import { SortState } from '@navikt/ds-react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { soekOppgaver } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { finnOgOppdaterOppgave } from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { FilterRad } from '~components/oppgavebenk/filtreringAvOppgaver/FilterRad'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import {
  hentMinOppgavelisteFilterFraLocalStorage,
  leggMinOppgavelisteFilterILocalsotrage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'
import { byggOppgaveSoekRequest } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { hentPagineringSizeFraLocalStorage } from '~components/oppgavebenk/utils/oppgaveHandlinger'
import {
  initialSortering,
  leggTilSorteringILocalStorage,
  OppgaveSortering,
  hentSorteringFraLocalStorage,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { SortKey } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { VStack } from '@navikt/ds-react'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const MinOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const [filter, setFilter] = useState<Filter>(hentMinOppgavelisteFilterFraLocalStorage())
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
    soekFetch(byggOppgaveSoekRequest(aktivFilter, sortering, aktivPage - 1, rowsPerPage, true), (result) => {
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
    leggMinOppgavelisteFilterILocalsotrage(filter)
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
    <VStack gap="space-24">
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
        oppgavelisteValg={OppgavelisteValg.MIN_OPPGAVELISTE}
      />
      {oppgaver.length >= 0 && !isPending(soekResult) ? (
        <Oppgaver
          oppgaver={oppgaver}
          totaltAntall={totaltAntall}
          saksbehandlereIEnhet={saksbehandlereIEnhet}
          oppdaterSaksbehandlerTildeling={oppdaterSaksbehandlerTildeling}
          oppdaterFrist={oppdaterFrist}
          oppdaterStatus={oppdaterStatus}
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
          pending: <Spinner label="Henter dine oppgaver" />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente dine oppgaver'}</ApiErrorAlert>,
        })
      )}
    </VStack>
  )
}
