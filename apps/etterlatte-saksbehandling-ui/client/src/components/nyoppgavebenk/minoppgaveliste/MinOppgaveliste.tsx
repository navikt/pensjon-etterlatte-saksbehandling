import { erOppgaveRedigerbar, OppgaveDTOny } from '~shared/api/oppgaverny'
import { Pagination, Select, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { FristHandlinger } from '~components/nyoppgavebenk/minoppgaveliste/FristHandlinger'
import React, { useState } from 'react'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { PaginationWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import {
  filtrerOppgaveStatus,
  OPPGAVESTATUSFILTER,
  OppgavestatusFilterKeys,
} from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HeaderPadding } from '~components/nyoppgavebenk/Oppgavelista'
import SaksoversiktLenke from '~components/nyoppgavebenk/SaksoversiktLenke'
import styled from 'styled-components'
import { RedigerSaksbehandler } from '../tildeling/RedigerSaksbehandler'

const SelectWrapper = styled.div`
  margin: 2rem 2rem 2rem 0rem;
  max-width: 20rem;
`

interface Props {
  oppgaver: OppgaveDTOny[]
  hentOppgaver: () => void
  oppdaterTildeling: (id: string, saksbehandler: string | null) => void
}

export const MinOppgaveliste = ({ oppgaver, hentOppgaver, oppdaterTildeling }: Props) => {
  const [oppgavestatusFilter, setOppgavestatusFilter] = useState<OppgavestatusFilterKeys>('UNDER_BEHANDLING')
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)

  const statusFiltrerteOppgaver = filtrerOppgaveStatus(oppgavestatusFilter, oppgaver)
  let paginerteOppgaver = statusFiltrerteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  return (
    <>
      <SelectWrapper>
        <Select
          label="Oppgavestatus"
          value={oppgavestatusFilter}
          onChange={(e) => setOppgavestatusFilter(e.target.value as OppgavestatusFilterKeys)}
        >
          {Object.entries(OPPGAVESTATUSFILTER).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>
      </SelectWrapper>
      {paginerteOppgaver.length > 0 ? (
        <div>
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
                <Table.HeaderCell scope="col">
                  <HeaderPadding>Frist</HeaderPadding>
                </Table.HeaderCell>
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
                        <FristHandlinger
                          orginalFrist={oppgave.frist}
                          oppgaveId={oppgave.id}
                          hentOppgaver={hentOppgaver}
                          erRedigerbar={erRedigerbar}
                          oppgaveVersjon={oppgave.versjon}
                          type={oppgave.type}
                        />
                      </Table.DataCell>
                      <Table.DataCell>
                        <SaksoversiktLenke fnr={oppgave.fnr} />
                      </Table.DataCell>
                      <Table.DataCell>
                        <OppgavetypeTag oppgavetype={oppgave.type} />
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
              count={Math.ceil(statusFiltrerteOppgaver.length / rowsPerPage)}
              size="small"
            />
            <p>
              Viser {(page - 1) * rowsPerPage + 1} - {(page - 1) * rowsPerPage + paginerteOppgaver.length} av{' '}
              {statusFiltrerteOppgaver.length} oppgaver
            </p>
            <select
              value={rowsPerPage}
              onChange={(e) => {
                setRowsPerPage(Number(e.target.value))
              }}
              title={'Antall oppgaver som vises'}
            >
              {[10, 20, 30, 40, 50].map((rowsPerPage) => (
                <option key={rowsPerPage} value={rowsPerPage}>
                  Vis {rowsPerPage} oppgaver
                </option>
              ))}
            </select>
          </PaginationWrapper>
        </div>
      ) : (
        <>Du har ingen oppgaver</>
      )}
    </>
  )
}
