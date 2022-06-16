import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Dokument } from './typer'

const colonner = ['Dato', 'Tittel', 'Status']

export const Dokumentliste = ({ dokumenter }: { dokumenter: Dokument[] }) => {
  return (
    <TableWrapper>
      <Table>
        <Table.Header className="test">
          <Table.Row>
            {colonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        {dokumenter.map((brev, i) => (
          <Table.Body key={i}>
            <Table.Row>
              <Table.DataCell key={`data${brev.dato}`}>{brev.dato}</Table.DataCell>
              <Table.DataCell key={`data${brev.tittel}`}>
                <Link>{brev.tittel}</Link>
              </Table.DataCell>
              <Table.DataCell key={`data${brev.status}`}>{brev.status}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        ))}
      </Table>
    </TableWrapper>
  )
}

export const TableWrapper = styled.div`
  .test {
    border-bottom: 1.5px solid #000000;
  }
`

export const Link = styled.div`
  cursor: pointer;
  text-decoration: underline;
  color: #0067c5;
`
