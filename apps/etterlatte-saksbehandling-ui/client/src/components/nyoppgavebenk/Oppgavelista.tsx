import { Pagination, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { OPPGAVESTATUSFILTER } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { isBefore } from 'date-fns'
import SaksoversiktLenke from '~components/oppgavebenken/handlinger/BrukeroversiktKnapp'

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

export const Oppgavelista = (props: {
  oppgaver: ReadonlyArray<OppgaveDTOny>
  hentOppgaver: () => void
  filtrerteOppgaver: ReadonlyArray<OppgaveDTOny>
}) => {
  const { oppgaver, hentOppgaver, filtrerteOppgaver } = props

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
                paginerteOppgaver.map(
                  ({
                    id,
                    status,
                    enhet,
                    type,
                    saksbehandler,
                    opprettet,
                    merknad,
                    sakType,
                    fnr,
                    frist,
                    sakId,
                    referanse,
                  }) => (
                    <Table.Row key={id}>
                      <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                      <Table.DataCell>
                        <FristWrapper fristHarPassert={!!frist && isBefore(new Date(frist), new Date())}>
                          {frist ? formaterStringDato(frist) : 'Ingen frist'}
                        </FristWrapper>
                      </Table.DataCell>
                      <Table.DataCell>
                        <SaksoversiktLenke fnr={fnr} />
                      </Table.DataCell>
                      <Table.DataCell>
                        {type ? <OppgavetypeTag oppgavetype={type} /> : <div>oppgaeveid {id}</div>}
                      </Table.DataCell>
                      <Table.DataCell>{sakType && <SaktypeTag sakType={sakType} />}</Table.DataCell>
                      <Table.DataCell>{merknad}</Table.DataCell>
                      <Table.DataCell>
                        {<span>{status ? OPPGAVESTATUSFILTER[status] ?? status : 'Ukjent'}</span>}
                      </Table.DataCell>
                      <Table.DataCell>{enhet}</Table.DataCell>
                      <Table.DataCell>
                        {saksbehandler ? (
                          <RedigerSaksbehandler
                            status={status}
                            saksbehandler={saksbehandler}
                            oppgaveId={id}
                            sakId={sakId}
                            hentOppgaver={hentOppgaver}
                          />
                        ) : (
                          <TildelSaksbehandler oppgaveId={id} hentOppgaver={hentOppgaver} />
                        )}
                      </Table.DataCell>
                      <Table.DataCell>
                        <HandlingerForOppgave
                          oppgavetype={type}
                          fnr={fnr}
                          saksbehandler={saksbehandler}
                          referanse={referanse}
                        />
                      </Table.DataCell>
                    </Table.Row>
                  )
                )}
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
              title={'Antall oppgaver som vises'}
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
