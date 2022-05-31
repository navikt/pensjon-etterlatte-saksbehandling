import { Link, Table } from '@navikt/ds-react'

interface BehandlingsKolonne {
  col: string
  value: string
}

interface Behandling {
  kolonner: BehandlingsKolonne[]
}
interface Sak {
  behandlinger: Behandling[]
}
export interface SakslisteProps {
  saker: Sak[]
}

const colonner = ['Opprettet', 'Type', 'Årsak', 'Status', 'Vedtaksdato', 'Resultat']

export const Saksliste = (props: SakslisteProps) => {
  return (
    <div>
      {props.saker.map((sak) => (
        <>
          <Table>
            <Table.Header>
              <Table.Row>
                {colonner.map((col) => (
                  <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {sak.behandlinger.map((behandling: Behandling, i: number) => (
                <Table.Row key={i}>
                  {behandling.kolonner.map((col, i) =>
                    i + 1 === behandling.kolonner.length ? (
                      <Table.DataCell key={col.col}>
                        <Link>{col.value}</Link>
                      </Table.DataCell>
                    ) : (
                      <Table.DataCell key={col.col}>{col.value}</Table.DataCell>
                    )
                  )}
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        </>
      ))}
    </div>
  )
}
