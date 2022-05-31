import { Link, Table } from '@navikt/ds-react'
import styled from 'styled-components'

const colonner = ['Dato', 'Tittel', 'Status']

export const Dokumentliste = (props: any) => {
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
        <Table.Body>
          {props.dokumenter.map((doc: any, i: number) => (
            <Table.Row key={i}>
              {doc.kolonner.map((col: any, i: number) =>
                col.col !== 'Tittel' ? (
                  <Table.DataCell className="tableCell" key={i}>
                    {col.value}
                  </Table.DataCell>
                ) : (
                  <Table.DataCell key={col.col}>
                    <Link href={col.link}>{col.value}</Link>
                  </Table.DataCell>
                )
              )}
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}

export const TableWrapper = styled.div`
  .test {
    border-bottom: 1.5px solid #000000;
  }
`
