import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Behandling } from './typer'

const colonner = ['Opprettet', 'Type', 'Årsak', 'Status', 'Vedtaksdato', 'Resultat']

export const Saksliste = ({
  saksliste,
  goToBehandling,
}: {
  saksliste: Behandling[]
  goToBehandling: (behandlingsId: string) => void
}) => {
  return (
    <div>
      <Table>
        <Table.Header>
          <Table.Row>
            {colonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>

        {saksliste.map((behandling, i) => (
          <Table.Body key={i}>
            <Table.Row>
              <Table.DataCell key={`data${behandling.opprettet}`}>{behandling.opprettet}</Table.DataCell>
              <Table.DataCell key={`data${behandling.type}`}>{behandling.type}</Table.DataCell>
              <Table.DataCell key={`data${behandling.årsak}`}>{behandling.årsak}</Table.DataCell>
              <Table.DataCell key={`data${behandling.status}`}>{behandling.status}</Table.DataCell>
              <Table.DataCell key={`data${behandling.vedtaksdato}`}>{behandling.vedtaksdato}</Table.DataCell>
              <Table.DataCell key={i}>
                <Link onClick={() => goToBehandling(behandling.id.toString())}>{behandling.resultat}</Link>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        ))}
      </Table>
    </div>
  )
}

export const Link = styled.div`
  cursor: pointer;
  text-decoration: underline;
  color: #0067c5;
`
