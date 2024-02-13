import { Alert } from '@navikt/ds-react'
import { OppgaveDTO } from '~shared/api/oppgaver'
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

export interface oppgaveListaProps {
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: string | null, versjon: number | null) => void
  oppgaver: OppgaveDTO[]
  oppdaterFrist: (id: string, nyfrist: string, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  totaltAntallOppgaver?: number
  erMinOppgaveliste: boolean
}

export const Oppgaver = ({
  oppdaterTildeling,
  oppgaver,
  oppdaterFrist,
  saksbehandlereIEnhet,
  totaltAntallOppgaver,
  erMinOppgaveliste,
}: oppgaveListaProps): ReactNode => {
  const [sortering, setSortering] = useState<OppgaveSortering>(hentSorteringFraLocalStorage())

  const sortertFrist = sorterFrist(sortering.fristSortering, oppgaver)
  const sorterteOppgaver = sorterFnr(sortering.fnrSortering, sortertFrist)

  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  let paginerteOppgaver = sorterteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && oppgaver.length > 0) setPage(1)
  }, [paginerteOppgaver, oppgaver])

  return (
    <>
      {paginerteOppgaver && paginerteOppgaver.length > 0 ? (
        <>
          <PagineringsKontroller page={page} setPage={setPage} antallSider={Math.ceil(oppgaver.length / rowsPerPage)} />

          <OppgaverTable
            oppgaver={paginerteOppgaver}
            oppdaterTildeling={oppdaterTildeling}
            erMinOppgaveliste={erMinOppgaveliste}
            oppdaterFrist={oppdaterFrist}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            setSortering={setSortering}
          />

          <PagineringsKontroller
            page={page}
            setPage={setPage}
            antallSider={Math.ceil(oppgaver.length / rowsPerPage)}
            raderPerSide={rowsPerPage}
            setRaderPerSide={setRowsPerPage}
            totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
              (page - 1) * rowsPerPage + paginerteOppgaver.length
            } av ${oppgaver.length} oppgaver ${
              totaltAntallOppgaver ? `(totalt ${totaltAntallOppgaver} oppgaver)` : ''
            }`}
          />
        </>
      ) : (
        <Alert variant="info">Ingen oppgaver</Alert>
      )}
    </>
  )
}
