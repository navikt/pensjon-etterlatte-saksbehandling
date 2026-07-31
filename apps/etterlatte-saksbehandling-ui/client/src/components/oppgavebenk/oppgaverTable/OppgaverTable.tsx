import React, { ReactNode } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'
import Spinner from '~shared/Spinner'

export enum SortKey {
  REGISTRERINGSDATO = 'registreringsdato',
  FRIST = 'frist',
  FNR = 'fnr',
}

const ANTALL_KOLONNER = 11

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist: (id: string, nyfrist: string) => void
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  sort: SortState | undefined
  handleSort: (sortKey: string) => void
  isLoading?: boolean
}

export const OppgaverTable = ({
  oppgaver,
  oppdaterTildeling,
  oppdaterFrist,
  oppdaterStatus,
  oppdaterMerknad,
  saksbehandlereIEnhet,
  sort,
  handleSort,
  isLoading,
}: Props): ReactNode => {
  return (
    <Table
      size="small"
      sort={sort && sort.direction !== 'none' ? { direction: sort.direction, orderBy: sort.orderBy } : undefined}
      onSortChange={handleSort}
    >
      <colgroup>
        <col style={{ width: '110px' }} />
        <col style={{ width: '140px' }} />
        <col style={{ width: '140px' }} />
        <col style={{ width: '190px' }} />
        <col style={{ width: '100px' }} />
        <col style={{ width: '250px' }} />
        <col style={{ width: '600px' }} />
        <col style={{ width: '250px' }} />
        <col style={{ width: '100px' }} />
        <col style={{ width: '250px' }} />
        <col style={{ width: '250px' }} />
      </colgroup>
      <OppgaverTableHeader />
      <Table.Body>
        {isLoading ? (
          <Table.Row>
            <Table.DataCell colSpan={ANTALL_KOLONNER}>
              <Spinner label="Henter oppgaver" />
            </Table.DataCell>
          </Table.Row>
        ) : (
          oppgaver?.map((oppgave: OppgaveDTO) => (
            <OppgaverTableRow
              key={oppgave.id}
              oppgave={oppgave}
              saksbehandlereIEnhet={saksbehandlereIEnhet}
              oppdaterTildeling={oppdaterTildeling}
              oppdaterFrist={oppdaterFrist}
              oppdaterStatus={oppdaterStatus}
              oppdaterMerknad={oppdaterMerknad}
            />
          ))
        )}
      </Table.Body>
    </Table>
  )
}
