import { Alert } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import { PagineringsKontroller } from '~components/oppgavebenk/oppgaver/PagineringsKontroller'
import {
  hentSorteringFraLocalStorage,
  OppgaveSortering,
  sorterGosysOppgaver,
} from '~components/oppgavebenk/utils/oppgaveSortering'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { hentPagineringSizeFraLocalStorage } from '~components/oppgavebenk/utils/oppgaveutils'
import { GosysOppgave } from '~shared/types/Gosys'
import { GosysOppgaverTable } from './GosysOppgaverTable'

export interface Props {
  oppgaver: GosysOppgave[]
  saksbehandlereIEnhet: Array<Saksbehandler>
  fnrFilter?: string
}

export const GosysOppgaver = ({ oppgaver, saksbehandlereIEnhet, fnrFilter }: Props): ReactNode => {
  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())

  const filtrerteOppgaver = fnrFilter ? oppgaver.filter(({ bruker }) => bruker?.ident === fnrFilter.trim()) : oppgaver
  const sorterteOppgaver = sorterGosysOppgaver(filtrerteOppgaver, sortering)

  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  let paginerteOppgaver = sorterteOppgaver

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && oppgaver.length > 0) setPage(1)
  }, [oppgaver, sorterteOppgaver])

  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  if (!paginerteOppgaver.length) return <Alert variant="info">Ingen oppgaver</Alert>

  return (
    <>
      <PagineringsKontroller
        page={page}
        setPage={setPage}
        antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
      />

      <GosysOppgaverTable
        oppgaver={paginerteOppgaver}
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
