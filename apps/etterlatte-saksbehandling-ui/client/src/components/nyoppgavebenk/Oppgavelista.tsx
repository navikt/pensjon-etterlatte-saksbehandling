import { Pagination, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { erOppgaveRedigerbar, OppgaveDTOny } from '~shared/api/oppgaverny'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { OPPGAVESTATUSFILTER } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { isBefore } from 'date-fns'
import SaksoversiktLenke from '~components/nyoppgavebenk/SaksoversiktLenke'
import { RedigerSaksbehandler } from './tildeling/RedigerSaksbehandler'

export const FristWrapper = styled.span<{ fristHarPassert: boolean }>`
  color: ${(p) => p.fristHarPassert && 'var(--a-text-danger)'};
`

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

// Spesiell håndtering for ferdigstilt status for oppgaver som medfører en ny oppgave til attestant
export function formaterOppgavestatus(oppgave: OppgaveDTOny): string {
  if (
    oppgave.status === 'FERDIGSTILT' &&
    (oppgave.type === 'FOERSTEGANGSBEHANDLING' ||
      oppgave.type === 'REVURDERING' ||
      oppgave.type === 'MANUELT_OPPHOER' ||
      oppgave.type === 'UNDERKJENT')
  ) {
    return 'Til attestering'
  }

  return oppgave.status ? OPPGAVESTATUSFILTER[oppgave.status] ?? oppgave.status : 'Ukjent'
}

export const Oppgavelista = (props: {
  oppgaver: ReadonlyArray<OppgaveDTOny>
  oppdaterTildeling: (id: string, saksbehandler: string | null) => void
  filtrerteOppgaver: ReadonlyArray<OppgaveDTOny>
}) => {
  const { oppgaver, oppdaterTildeling, filtrerteOppgaver } = props

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
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
                <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
                <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
                <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
                <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
                <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
                <Table.HeaderCell scope="col">Status</Table.HeaderCell>
                <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
                <Table.HeaderCell scope="col">
                  <HeaderPadding>Saksbehandler</HeaderPadding>
                </Table.HeaderCell>
                <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {paginerteOppgaver &&
                paginerteOppgaver.map((oppgave) => {
                  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

                  return (
                    <Table.Row key={oppgave.id}>
                      <Table.HeaderCell>{formaterStringDato(oppgave.opprettet)}</Table.HeaderCell>
                      <Table.DataCell>
                        <FristWrapper
                          fristHarPassert={!!oppgave.frist && isBefore(new Date(oppgave.frist), new Date())}
                        >
                          {oppgave.frist ? formaterStringDato(oppgave.frist) : 'Ingen frist'}
                        </FristWrapper>
                      </Table.DataCell>
                      <Table.DataCell>
                        <SaksoversiktLenke fnr={oppgave.fnr} />
                      </Table.DataCell>
                      <Table.DataCell>
                        {oppgave.type ? (
                          <OppgavetypeTag oppgavetype={oppgave.type} />
                        ) : (
                          <div>oppgaveid {oppgave.id}</div>
                        )}
                      </Table.DataCell>
                      <Table.DataCell>{oppgave.sakType && <SaktypeTag sakType={oppgave.sakType} />}</Table.DataCell>
                      <Table.DataCell>{oppgave.merknad}</Table.DataCell>
                      <Table.DataCell>{formaterOppgavestatus(oppgave)}</Table.DataCell>
                      <Table.DataCell>{oppgave.enhet}</Table.DataCell>
                      <Table.DataCell>
                        <RedigerSaksbehandler
                          saksbehandler={oppgave.saksbehandler}
                          oppgaveId={oppgave.id}
                          sakId={oppgave.sakId}
                          oppdaterTildeling={oppdaterTildeling}
                          erRedigerbar={erRedigerbar}
                          versjon={oppgave.versjon}
                          type={oppgave.type}
                        />
                      </Table.DataCell>
                      <Table.DataCell>
                        <HandlingerForOppgave oppgave={oppgave} />
                      </Table.DataCell>
                    </Table.Row>
                  )
                })}
            </Table.Body>
          </Table>

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
        <>Ingen oppgaver</>
      )}
    </>
  )
}
