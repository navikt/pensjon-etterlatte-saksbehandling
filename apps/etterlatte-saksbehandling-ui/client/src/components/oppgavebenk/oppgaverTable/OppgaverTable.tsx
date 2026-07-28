import React, { ReactNode } from 'react'
import { SortState, Table } from '@navikt/ds-react'
import { OppgaverTableHeader } from '~components/oppgavebenk/oppgaverTable/OppgaverTableHeader'
import { OppgaverTableRow } from '~components/oppgavebenk/oppgaverTable/OppgaverTableRow'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'

export enum SortKey {
  REGISTRERINGSDATO = 'registreringsdato',
  FRIST = 'frist',
  FNR = 'fnr',
}

interface Props {
  oppgaver: ReadonlyArray<OppgaveDTO>
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) => void
  oppdaterFrist: (id: string, nyfrist: string) => void
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  sort: SortState | undefined
  handleSort: (sortKey: string) => void
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
}: Props): ReactNode => {
  return (
    <Table
      size="small"
      sort={sort && sort.direction !== 'none' ? { direction: sort.direction, orderBy: sort.orderBy } : undefined}
      onSortChange={handleSort}
    >
      <OppgaverTableHeader />
      <Table.Body>
        {oppgaver?.map((oppgave: OppgaveDTO) => (
          <OppgaverTableRow
            key={oppgave.id}
            oppgave={oppgave}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            oppdaterTildeling={oppdaterTildeling}
            oppdaterFrist={oppdaterFrist}
            oppdaterStatus={oppdaterStatus}
            oppdaterMerknad={oppdaterMerknad}
          />
        ))}
      </Table.Body>
    </Table>
  )
}
