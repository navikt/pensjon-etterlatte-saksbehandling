import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/api/oppgaver'
import { Alert, Pagination, Table, UNSAFE_Combobox } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { FristHandlinger } from '~components/nyoppgavebenk/minoppgaveliste/FristHandlinger'
import React, { useEffect, useState } from 'react'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { HeaderPadding, PaginationWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import { filtrerOppgaveStatus, OPPGAVESTATUSFILTER } from '~components/nyoppgavebenk/Oppgavelistafiltre'
import SaksoversiktLenke from '~components/nyoppgavebenk/SaksoversiktLenke'
import { RedigerSaksbehandler } from '../tildeling/RedigerSaksbehandler'

interface Props {
  oppgaver: OppgaveDTO[]
  hentOppgaver: () => void
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
}

export const MinOppgaveliste = ({ oppgaver, hentOppgaver, oppdaterTildeling }: Props) => {
  const [oppgavestatuserValgt, setOppgavestatuserValgt] = useState<Array<string>>(['UNDER_BEHANDLING'])
  const [page, setPage] = useState<number>(1)
  const [rowsPerPage, setRowsPerPage] = useState<number>(10)
  const [paginerteOppgaver, setPaginerteOppgaver] = useState<Array<OppgaveDTO>>(
    filtrerOppgaveStatus(oppgavestatuserValgt, oppgaver)
  )

  const onOppgavestatusSelected = (option: string, isSelected: boolean) => {
    let nyOppgavestatusSelected: Array<string>

    if (isSelected) {
      nyOppgavestatusSelected = [...oppgavestatuserValgt, option]
    } else {
      nyOppgavestatusSelected = [...oppgavestatuserValgt.filter((val) => val !== option)]
    }

    setOppgavestatuserValgt(nyOppgavestatusSelected)
  }

  useEffect(() => {
    let filtrerteOppgaver = filtrerOppgaveStatus(oppgavestatuserValgt, oppgaver)
    filtrerteOppgaver = filtrerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)
    setPaginerteOppgaver(filtrerteOppgaver)
  }, [oppgavestatuserValgt, oppgaver])

  return (
    <>
      <UNSAFE_Combobox
        label="Oppgavestatus"
        options={OPPGAVESTATUSFILTER}
        selectedOptions={oppgavestatuserValgt}
        onToggleSelected={(option, isSelected) => onOppgavestatusSelected(option, isSelected)}
        isMultiSelect
      />

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
                      <Table.DataCell>{oppgave.status ? oppgave.status : 'Ukjent'}</Table.DataCell>
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
              count={Math.ceil(paginerteOppgaver.length / rowsPerPage)}
              size="small"
            />
            <p>
              Viser {(page - 1) * rowsPerPage + 1} - {(page - 1) * rowsPerPage + paginerteOppgaver.length} av{' '}
              {paginerteOppgaver.length} oppgaver
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
        </div>
      ) : (
        <Alert variant="info">Du har ingen oppgaver</Alert>
      )}
    </>
  )
}
