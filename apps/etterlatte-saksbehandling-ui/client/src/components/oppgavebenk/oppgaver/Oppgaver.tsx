import { Alert } from '@navikt/ds-react'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import React, { ReactNode, useEffect, useState } from 'react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/oppgaver/PagineringsKontroller'
import {
  hentSorteringFraLocalStorage,
  OppgaveSortering,
  sorterFnr,
  sorterFrist,
} from '~components/oppgavebenk/oppgaverTable/oppgavesortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { hentPagineringSizeFraLocalStorage } from '~components/oppgavebenk/utils/oppgaveutils'
import { Filter, filtrerOppgaver } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'

export interface OppgavelisteProps {
  oppgaver: OppgaveDTO[]
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterFrist?: (id: string, nyfrist: string, versjon: number | null) => void
  filter?: Filter
}

export const Oppgaver = ({
  oppgaver,
  oppdaterTildeling,
  saksbehandlereIEnhet,
  oppdaterFrist,
  filter,
}: OppgavelisteProps): ReactNode => {
  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())
  const filtrerteOppgaver = filter
    ? filtrerOppgaver(
        filter.enhetsFilter,
        filter.fristFilter,
        filter.saksbehandlerFilter,
        filter.ytelseFilter,
        filter.oppgavestatusFilter,
        filter.oppgavetypeFilter,
        [...oppgaver],
        filter.fnrFilter
      )
    : oppgaver

  const sortertFrist = sorterFrist(sortering.fristSortering, filtrerteOppgaver)
  const sorterteOppgaver = sorterFnr(sortering.fnrSortering, sortertFrist)
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  let paginerteOppgaver = sorterteOppgaver
  useEffect(() => {
    if (paginerteOppgaver.length === 0 && filtrerteOppgaver.length > 0) setPage(1)
  }, [sorterteOppgaver, filtrerteOppgaver])

  if (!paginerteOppgaver.length) return <Alert variant="info">Ingen oppgaver</Alert>

  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  if (!paginerteOppgaver.length) return <Alert variant="info">Ingen oppgaver</Alert>

  return (
    <>
      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
      />

      <OppgaverTable
        oppgaver={paginerteOppgaver}
        oppdaterTildeling={oppdaterTildeling}
        oppdaterFrist={oppdaterFrist}
        saksbehandlereIEnhet={saksbehandlereIEnhet}
        setSortering={setSortering}
      />

      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
        raderPerSide={rowsPerPage}
        setRaderPerSide={setRowsPerPage}
        totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
          (page - 1) * rowsPerPage + paginerteOppgaver.length
        } av ${filtrerteOppgaver.length} oppgaver ${oppgaver.length ? `(totalt ${oppgaver.length} oppgaver)` : ''}`}
      />
    </>
  )
}
