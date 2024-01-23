import { OppgaveDTO } from '~shared/api/oppgaver'
import { Alert } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { Filter } from '~components/oppgavebenk/Oppgavelistafiltre'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { PagineringsKontroller } from '~components/oppgavebenk/PagineringsKontroller'

interface Props {
  filtrerteOppgaver: OppgaveDTO[]
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  hentOppgaver: () => void
  filter: Filter
  setFilter: (filter: Filter) => void
}

export const MinOppgaveliste = ({ filtrerteOppgaver, oppdaterTildeling, hentOppgaver, filter, setFilter }: Props) => {
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)

  let paginerteOppgaver = filtrerteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  useEffect(() => {
    if (paginerteOppgaver.length === 0 && filtrerteOppgaver.length > 0) setPage(1)
  }, [paginerteOppgaver, filtrerteOppgaver])

  return (
    <>
      <VelgOppgavestatuser
        value={filter.oppgavestatusFilter}
        onChange={(oppgavestatusFilter) => setFilter({ ...filter, oppgavestatusFilter })}
      />

      {paginerteOppgaver.length > 0 ? (
        <>
          <OppgaverTable
            oppgaver={paginerteOppgaver}
            oppdaterTildeling={oppdaterTildeling}
            erMinOppgaveliste={true}
            hentOppgaver={hentOppgaver}
          />

          <PagineringsKontroller
            page={page}
            setPage={setPage}
            antallSider={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
            raderPerSide={rowsPerPage}
            setRaderPerSide={setRowsPerPage}
            totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
              (page - 1) * rowsPerPage + paginerteOppgaver.length
            } av ${filtrerteOppgaver.length} oppgaver`}
          />
        </>
      ) : (
        <Alert variant="info">Du har ingen oppgaver</Alert>
      )}
    </>
  )
}
