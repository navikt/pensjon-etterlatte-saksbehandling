import { Table } from '@navikt/ds-react'

interface BehandlingsKolonne {
  col: string
  value: string
}

interface Behandling {
  kolonner: BehandlingsKolonne[]
}
interface Sak {
  behandlinger: Behandling[]
  name: string
}
export interface SakslisteProps {
  saker: Sak[]
}

export const Saksliste = (props: SakslisteProps) => {
  return (
    <div>
      {props.saker.map((sak) => (
        <>
          <h2>{sak.name}</h2>
          <Table>
            <Table.Header>
              <Table.Row>
                {props.saker[0].behandlinger[0].kolonner.map((col) => (
                  <Table.HeaderCell key={`header${col.col}`}>{col.col}</Table.HeaderCell>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {sak.behandlinger.map((behandling: Behandling, i: number) => (
                <Table.Row key={i}>
                  {behandling.kolonner.map((col) => (
                    <Table.DataCell key={col.col}>{col.value}</Table.DataCell>
                  ))}
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        </>
      ))}
    </div>
  )
}
