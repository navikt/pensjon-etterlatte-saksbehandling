import { Alert } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/oppgaver/PagineringsKontroller'
import {
  hentSorteringFraLocalStorage,
  OppgaveSortering,
  sorterOppgaver,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { hentPagineringSizeFraLocalStorage } from '~components/oppgavebenk/utils/oppgaveutils'
import { filtrerOppgaver } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/types/oppgave'

export interface Props {
  oppgaver: OppgaveDTO[]
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppdaterSaksbehandlerTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist?: (id: string, nyfrist: string) => void
  filter?: Filter
}

export const Oppgaver = ({
  oppgaver,
  saksbehandlereIEnhet,
  oppdaterSaksbehandlerTildeling,
  oppdaterFrist,
  filter,
}: Props): ReactNode => {
  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())
  const filtrerteOppgaver = filter
    ? filtrerOppgaver(
        filter.sakEllerFnrFilter,
        filter.enhetsFilter,
        filter.fristFilter,
        filter.saksbehandlerFilter,
        filter.ytelseFilter,
        filter.oppgavestatusFilter,
        filter.oppgavetypeFilter,
        [...oppgaver]
      )
    : oppgaver

  const sorterteOppgaver = sorterOppgaver(filtrerteOppgaver, sortering)

  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  let paginerteOppgaver = sorterteOppgaver

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && filtrerteOppgaver.length > 0) setPage(1)
  }, [sorterteOppgaver, filtrerteOppgaver])

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
        oppdaterTildeling={oppdaterSaksbehandlerTildeling}
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
