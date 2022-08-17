import { Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { AarsaksTyper, IBehandlingsammendrag } from './typer'
import { formatterStringDato, upperCaseFirst } from "../../utils/formattering";

const colonner = ['Opprettet', 'Type', 'Årsak', 'Status', 'Vedtaksdato', 'Resultat']

export const Saksliste = ({
  behandlinger, goToBehandling,
}: {
  behandlinger: IBehandlingsammendrag[]
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

        {behandlinger.map((behandling, i) => (
          <Table.Body key={i}>
            <Table.Row>
              <Table.DataCell
                key={`data${behandling.behandlingOpprettet}`}>{formatterStringDato(behandling.behandlingOpprettet)}</Table.DataCell>
              <Table.DataCell
                key={`data${behandling.behandlingType}`}>{upperCaseFirst(behandling.behandlingType)}</Table.DataCell>
              <Table.DataCell key={`data${behandling.aarsak}`}>{mapAarrsak(behandling.aarsak)}</Table.DataCell>
              <Table.DataCell key={`data${behandling.status}`}>{upperCaseFirst(behandling.status)}</Table.DataCell>
              <Table.DataCell key={'vedtaksdato'}>Vedtaksdato her</Table.DataCell>
              <Table.DataCell key={i}>
                <Link onClick={() => goToBehandling(behandling.id.toString())}>Gå til behandling</Link>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        ))}
      </Table>
    </div>
  )
}

function mapAarrsak(aarsak: AarsaksTyper) {
  switch (aarsak) {
    case AarsaksTyper.MANUELT_OPPHOER:
      return 'Manuelt opphør'
    case AarsaksTyper.SOEKER_DOD:
      return 'Søker er død'
    case AarsaksTyper.SOEKNAD:
      return 'Søknad'
  }

}

export const Link = styled.div`
  cursor: pointer;
  text-decoration: underline;
  color: #0067c5;
`
