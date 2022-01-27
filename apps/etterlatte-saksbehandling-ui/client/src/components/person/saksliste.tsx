import { Table } from '@navikt/ds-react'

interface SakRow {
  col: string
  value: string
}
interface Sak {
  verdi: SakRow[]
}
export interface SakslisteProps {
  saker: Sak[]
}

export const Saksliste = (props: SakslisteProps) => {
  return (
    <Table>
      <Table.Header>
          <Table.Row>
            {props.saker[0].verdi.map((col) => (
              <Table.HeaderCell key={`header${col.col}`}>{col.col}</Table.HeaderCell>
            ))}
          </Table.Row>
      </Table.Header>
      <Table.Body>
        {props.saker.map((el: Sak, i: number) => (
          <Table.Row key={i}>
            {el.verdi.map((col) => (
              <Table.DataCell key={col.col}>{col.value}</Table.DataCell>
            ))}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}
