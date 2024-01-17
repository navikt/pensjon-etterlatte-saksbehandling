import { Alert, Pagination, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/api/oppgaver'
import React, { ReactNode, useEffect, useState } from 'react'
import styled from 'styled-components'
import { OPPGAVESTATUSFILTER } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import SaksoversiktLenke from '~components/nyoppgavebenk/SaksoversiktLenke'
import { RedigerSaksbehandler } from './tildeling/RedigerSaksbehandler'
import { FristWrapper } from '~components/nyoppgavebenk/FristWrapper'

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

  // Filtrere bort ferdigstilte/avbrutte søknader på en spesifik saksbehandler / generelt
  // Sette opp combobox for velging av oppgavestatus

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
                        <FristWrapper dato={oppgave.frist} />
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
                      <Table.DataCell>
                        {oppgave.status ? OPPGAVESTATUSFILTER[oppgave.status] ?? oppgave.status : 'Ukjent'}
                      </Table.DataCell>
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
