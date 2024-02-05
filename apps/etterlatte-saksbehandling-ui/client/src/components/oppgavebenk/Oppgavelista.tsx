import { Alert } from '@navikt/ds-react'
import { OppgaveDTO } from '~shared/api/oppgaver'
import React, { Dispatch, ReactNode, SetStateAction, useEffect, useState } from 'react'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { PagineringsKontroller } from '~components/oppgavebenk/PagineringsKontroller'
import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

interface Props {
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  filtrerteOppgaver: ReadonlyArray<OppgaveDTO>
  hentOppgaver: () => void
  filter: Filter
  setFilter: Dispatch<SetStateAction<Filter>>
  totaltAntallOppgaver?: number
  erMinOppgaveliste: boolean
}

export const Oppgavelista = ({
  oppdaterTildeling,
  filtrerteOppgaver,
  hentOppgaver,
  filter,
  setFilter,
  totaltAntallOppgaver,
  erMinOppgaveliste,
}: Props): ReactNode => {
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)

  let paginerteOppgaver = filtrerteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && filtrerteOppgaver.length > 0) setPage(1)
  }, [paginerteOppgaver, filtrerteOppgaver])

  return (
    <>
      {paginerteOppgaver && paginerteOppgaver.length > 0 ? (
        <>
          <OppgaverTable
            oppgaver={paginerteOppgaver}
            oppdaterTildeling={oppdaterTildeling}
            erMinOppgaveliste={erMinOppgaveliste}
            hentOppgaver={hentOppgaver}
            filter={filter}
            setFilter={setFilter}
          />

          <PagineringsKontroller
            page={page}
            setPage={setPage}
            antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
            raderPerSide={rowsPerPage}
            setRaderPerSide={setRowsPerPage}
            totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
              (page - 1) * rowsPerPage + paginerteOppgaver.length
            } av ${filtrerteOppgaver.length} oppgaver ${
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
