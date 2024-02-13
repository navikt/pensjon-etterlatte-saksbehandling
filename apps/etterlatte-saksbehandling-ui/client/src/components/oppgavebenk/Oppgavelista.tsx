import { Alert } from '@navikt/ds-react'
import { OppgaveDTO, Saksbehandler } from '~shared/api/oppgaver'
import React, { Dispatch, ReactNode, SetStateAction, useEffect, useState } from 'react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/PagineringsKontroller'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { hentPagineringSizeFraLocalStorage } from '~components/oppgavebenk/oppgaveutils'

export interface oppgaveListaProps {
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterFrist: (id: string, nyfrist: string, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  filter: Filter
  setFilter: Dispatch<SetStateAction<Filter>>
  totaltAntallOppgaver?: number
  erMinOppgaveliste: boolean
}

export const Oppgavelista = ({
  oppdaterTildeling,
  oppgaver,
  oppdaterFrist,
  saksbehandlereIEnhet,
  filter,
  setFilter,
  totaltAntallOppgaver,
  erMinOppgaveliste,
}: oppgaveListaProps): ReactNode => {
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(hentPagineringSizeFraLocalStorage())

  let paginerteOppgaver = oppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && oppgaver.length > 0) setPage(1)
  }, [paginerteOppgaver, oppgaver])

  return (
    <>
      {paginerteOppgaver && paginerteOppgaver.length > 0 ? (
        <>
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
          <OppgaverTable
            oppgaver={paginerteOppgaver}
            oppdaterTildeling={oppdaterTildeling}
            erMinOppgaveliste={erMinOppgaveliste}
            oppdaterFrist={oppdaterFrist}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            filter={filter}
            setFilter={setFilter}
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
