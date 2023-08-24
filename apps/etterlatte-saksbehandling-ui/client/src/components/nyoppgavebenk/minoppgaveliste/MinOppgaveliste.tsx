import { erOppgaveRedigerbar, OppgaveDTOny } from '~shared/api/oppgaverny'
import { Pagination, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { FristHandlinger } from '~components/nyoppgavebenk/minoppgaveliste/FristHandlinger'
import React, { useState } from 'react'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import SaksoversiktLenke from '~components/oppgavebenken/handlinger/BrukeroversiktKnapp'
import { PaginationWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import { OPPGAVESTATUSFILTER } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HeaderPadding } from '~components/nyoppgavebenk/Oppgavelista'

export const MinOppgaveliste = (props: { oppgaver: ReadonlyArray<OppgaveDTOny>; hentOppgaver: () => void }) => {
  const { oppgaver, hentOppgaver } = props
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)

  let paginerteOppgaver = oppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  return (
    <>
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
                paginerteOppgaver.map(
                  ({
                    id,
                    status,
                    type,
                    enhet,
                    saksbehandler,
                    opprettet,
                    merknad,
                    sakType,
                    fnr,
                    frist,
                    sakId,
                    referanse,
                    beskrivelse,
                    gjelder,
                       versjon
                  }) => {
                    const erRedigerbar = erOppgaveRedigerbar(status, type)
                    return (
                      <Table.Row key={id}>
                        <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                        <Table.DataCell>
                          <FristHandlinger
                            orginalFrist={frist}
                            oppgaveId={id}
                            hentOppgaver={hentOppgaver}
                            erRedigerbar={erRedigerbar}
                            oppgaveVersjon={versjon}
                          />
                        </Table.DataCell>
                        <Table.DataCell>
                          <SaksoversiktLenke fnr={fnr} />
                        </Table.DataCell>
                        <Table.DataCell>
                          <OppgavetypeTag oppgavetype={type} />
                        </Table.DataCell>
                        <Table.DataCell>{sakType && <SaktypeTag sakType={sakType} />}</Table.DataCell>
                        <Table.DataCell>{merknad}</Table.DataCell>
                        <Table.DataCell>
                          {<span>{status ? OPPGAVESTATUSFILTER[status] ?? status : 'Ukjent'}</span>}
                        </Table.DataCell>
                        <Table.DataCell>{enhet}</Table.DataCell>
                        <Table.DataCell>
                          {saksbehandler && (
                            <RedigerSaksbehandler
                              saksbehandler={saksbehandler}
                              oppgaveId={id}
                              sakId={sakId}
                              hentOppgaver={hentOppgaver}
                              erRedigerbar={erRedigerbar}
                              oppgaveVersjon={versjon}
                            />
                          )}
                        </Table.DataCell>
                        <Table.DataCell>
                          <HandlingerForOppgave
                            oppgavetype={type}
                            oppgavestatus={status}
                            opprettet={opprettet}
                            frist={frist}
                            fnr={fnr}
                            saktype={sakType}
                            enhet={enhet}
                            saksbehandler={saksbehandler}
                            referanse={referanse}
                            beskrivelse={beskrivelse}
                            gjelder={gjelder}
                          />
                        </Table.DataCell>
                      </Table.Row>
                    )
                  }
                )}
            </Table.Body>
          </Table>
          <PaginationWrapper>
            <Pagination
              page={page}
              onPageChange={setPage}
              count={Math.ceil(oppgaver.length / rowsPerPage)}
              size="small"
            />
            <p>
              Viser {(page - 1) * rowsPerPage + 1} - {(page - 1) * rowsPerPage + paginerteOppgaver.length} av{' '}
              {oppgaver.length} oppgaver
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
