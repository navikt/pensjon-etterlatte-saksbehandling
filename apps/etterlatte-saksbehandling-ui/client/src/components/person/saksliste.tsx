import { Table, Link } from '@navikt/ds-react'
import { AarsaksTyper, IBehandlingsammendrag } from './typer'
import { formaterStringDato, formaterEnumTilLesbarString } from '~utils/formattering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

const colonner = ['Opprettet', 'Type', 'Årsak', 'Status', 'Virkningstidspunkt', 'Vedtaksdato', 'Resultat']

export const Saksliste = ({ behandlinger }: { behandlinger: IBehandlingsammendrag[] }) => {
  return (
    <div>
      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            {colonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {behandlinger.map((behandling, i) => (
            <Table.Row key={i} shadeOnHover={false}>
              <Table.DataCell key={`data${behandling.behandlingOpprettet}`}>
                {formaterStringDato(behandling.behandlingOpprettet)}
              </Table.DataCell>
              <Table.DataCell key={`data${behandling.behandlingType}`}>
                {formaterEnumTilLesbarString(behandling.behandlingType)}
              </Table.DataCell>
              <Table.DataCell key={`data${behandling.aarsak}`}>{mapAarsak(behandling.aarsak)}</Table.DataCell>
              <Table.DataCell key={`data${behandling.status}`}>
                {formaterEnumTilLesbarString(endringStatusNavn(behandling.status))}
              </Table.DataCell>
              <Table.DataCell key={'virkningstidspunkt'}>
                {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt!!.dato) : ''}
              </Table.DataCell>
              {
                //todo: legg inn vedtaksdato/iversettelsesdato og resultat når det er klart
              }
              <Table.DataCell key={'vedtaksdato'}></Table.DataCell>
              <Table.DataCell key={i}>
                <Link href={`/behandling/${behandling.id}/soeknadsoversikt`}>Gå til behandling</Link>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  )
}

function mapAarsak(aarsak: AarsaksTyper) {
  switch (aarsak) {
    case AarsaksTyper.MANUELT_OPPHOER:
      return 'Manuelt opphør'
    case AarsaksTyper.SOEKER_DOD:
      return 'Søker er død'
    case AarsaksTyper.SOEKNAD:
      return 'Søknad'
  }
}

function endringStatusNavn(status: IBehandlingStatus) {
  switch (status) {
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Til attestering'
    case IBehandlingStatus.ATTESTERT:
      return 'Iverksatt'
    default:
      return status
  }
}
