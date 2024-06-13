import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import { SortKey } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'

export const OppgaverTableHeader = (): ReactNode => {
  return (
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell scope="col">Sak ID</Table.HeaderCell>
        <Table.ColumnHeader scope="col" sortKey={SortKey.REGISTRERINGSDATO} sortable>
          Reg.dato
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey={SortKey.FRIST} sortable>
          Frist
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey={SortKey.FNR} sortable>
          Fødselsnummer
        </Table.ColumnHeader>
        <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
        <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
        <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
        <Table.HeaderCell scope="col">Status</Table.HeaderCell>
        <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
        <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
        <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
      </Table.Row>
    </Table.Header>
  )
}
