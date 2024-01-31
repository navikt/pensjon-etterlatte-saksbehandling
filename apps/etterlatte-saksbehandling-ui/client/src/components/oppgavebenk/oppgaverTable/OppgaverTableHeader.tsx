import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { SortKey } from '~components/oppgavebenk/oppgaverTable/OppgaverTable'

export const OppgaverTableHeader = (): ReactNode => {
  return (
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
        <Table.ColumnHeader scope="col" sortKey={SortKey.FRIST} sortable>
          Frist
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey={SortKey.FNR} sortable>
          Fødselsnummer
        </Table.ColumnHeader>
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
  )
}

const HeaderPadding = styled.span`
  padding-left: 20px;
`
