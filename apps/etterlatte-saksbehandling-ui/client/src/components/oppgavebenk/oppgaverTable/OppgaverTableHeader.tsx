import React, { ReactNode } from 'react'
import { Table } from '@navikt/ds-react'
import styled from 'styled-components'

export const OppgaverTableHeader = (): ReactNode => {
  return (
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
  )
}

const HeaderPadding = styled.span`
  padding-left: 20px;
`
