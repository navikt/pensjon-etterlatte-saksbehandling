import { Alert, Pagination } from '@navikt/ds-react'
import { OppgaveDTO } from '~shared/api/oppgaver'
import React, { ReactNode, useEffect, useState } from 'react'
import styled from 'styled-components'
import { OppgaverTable } from '~components/nyoppgavebenk/oppgaverTable/OppgaverTable'

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  filtrerteOppgaver: ReadonlyArray<OppgaveDTO>
}

export const Oppgavelista = ({ oppgaver, oppdaterTildeling, filtrerteOppgaver }: Props): ReactNode => {
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
          <OppgaverTable oppgaver={paginerteOppgaver} oppdaterTildeling={oppdaterTildeling} erMinOppgaveliste={false} />

          <PaginationWrapper>
            <Pagination
              page={page}
              onPageChange={setPage}
              count={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
              size="small"
            />
            <p>
              Viser {(page - 1) * rowsPerPage + 1} - {(page - 1) * rowsPerPage + paginerteOppgaver.length} av{' '}
              {filtrerteOppgaver.length} oppgaver (totalt {oppgaver.length} oppgaver)
            </p>
            <select
              value={rowsPerPage}
              onChange={(e) => {
                setRowsPerPage(Number(e.target.value))
              }}
              title="Antall oppgaver som vises"
            >
              {[10, 20, 30, 40, 50].map((rowsPerPage) => (
                <option key={rowsPerPage} value={rowsPerPage}>
                  Vis {rowsPerPage} oppgaver
                </option>
              ))}
            </select>
          </PaginationWrapper>
        </>
      ) : (
        <Alert variant="info">Ingen oppgaver</Alert>
      )}
    </>
  )
}

export const PaginationWrapper = styled.div`
  display: flex;
  gap: 0.5em;
  flex-wrap: wrap;
  margin: 0.5em 0;

  > p {
    margin: 0;
    line-height: 32px;
  }
`

export const HeaderPadding = styled.span`
  padding-left: 20px;
`
