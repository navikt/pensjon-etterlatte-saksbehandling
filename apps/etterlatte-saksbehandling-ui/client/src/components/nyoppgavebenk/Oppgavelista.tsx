import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny } from '~shared/api/oppgaverny'

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny> }) => {
  const { oppgaver } = props
  return (
    <div>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fnr</Table.HeaderCell>
            <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
            <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
            <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
            <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {oppgaver &&
            oppgaver.map(({ id, status, enhet, type, saksbehandler, opprettet, merknad, sakType, fnr, frist }) => (
              <Table.Row key={id}>
                <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                <Table.HeaderCell>{fnr ? fnr : 'ikke fnr, må migreres'}</Table.HeaderCell>
                <Table.DataCell>{type}</Table.DataCell>
                <Table.DataCell>{status}</Table.DataCell>
                <Table.DataCell>{merknad}</Table.DataCell>
                <Table.DataCell>{enhet}</Table.DataCell>
                <Table.DataCell>
                  {saksbehandler ? (
                    <RedigerSaksbehandler saksbehandler={saksbehandler} oppgaveId={id} />
                  ) : (
                    <TildelSaksbehandler oppgaveId={id} />
                  )}
                </Table.DataCell>
                <Table.DataCell>{sakType ? sakType : 'Ingen saktype, må migreres'}</Table.DataCell>
                <Table.DataCell>{frist ? frist : 'Ingen frist'}</Table.DataCell>
              </Table.Row>
            ))}
        </Table.Body>
      </Table>
    </div>
  )
}
