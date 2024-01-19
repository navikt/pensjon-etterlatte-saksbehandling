import { OppgaveDTO } from '~shared/api/oppgaver'
import { Alert } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { filtrerOppgaveStatus } from '~components/oppgavebenk/Oppgavelistafiltre'
import { OppgaverTable } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'
import { PagineringsKontroller } from '~components/oppgavebenk/PagineringsKontroller'

interface Props {
  oppgaver: OppgaveDTO[]
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  hentOppgaver: () => void
}

export const MinOppgaveliste = ({ oppgaver, oppdaterTildeling, hentOppgaver }: Props) => {
  const [oppgavestatuserValgt, setOppgavestatuserValgt] = useState<Array<string>>(['UNDER_BEHANDLING'])
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)
  const [paginerteOppgaver, setPaginerteOppgaver] = useState<Array<OppgaveDTO>>(
    filtrerOppgaveStatus(oppgavestatuserValgt, oppgaver)
  )

  useEffect(() => {
    let filtrerteOppgaver = filtrerOppgaveStatus(oppgavestatuserValgt, oppgaver)
    filtrerteOppgaver = filtrerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)
    setPaginerteOppgaver(filtrerteOppgaver)
  }, [oppgavestatuserValgt, oppgaver])

  return (
    <>
      <VelgOppgavestatuser value={oppgavestatuserValgt} onChange={setOppgavestatuserValgt} />

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
            antallSider={Math.ceil(paginerteOppgaver.length / rowsPerPage)}
            raderPerSide={rowsPerPage}
            setRaderPerSide={setRowsPerPage}
            totalAvOppgaverTeksts={`Viser ${(page - 1) * rowsPerPage + 1} - ${
              (page - 1) * rowsPerPage + paginerteOppgaver.length
            } av ${paginerteOppgaver.length} oppgaver`}
          />
        </>
      ) : (
        <Alert variant="info">Du har ingen oppgaver</Alert>
      )}
    </>
  )
}
