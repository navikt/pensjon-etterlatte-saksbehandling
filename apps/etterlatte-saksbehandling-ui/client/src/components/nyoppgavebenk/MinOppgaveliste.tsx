import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { useAppSelector } from '~store/Store'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'

export const MinOppgaveliste = (props: { oppgaver: ReadonlyArray<OppgaveDTOny> }) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const { oppgaver } = props

  const mineOppgaver = oppgaver.filter((o) => o.saksbehandler === user.ident)
  return (
    <>
      {mineOppgaver.length > 0 ? (
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
              {mineOppgaver &&
                mineOppgaver.map(
                  ({ id, status, enhet, type, saksbehandler, opprettet, merknad, sakType, fnr, frist }) => (
                    <Table.Row key={id}>
                      <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                      <Table.HeaderCell>{fnr ? fnr : 'ikke fnr, må migreres'}</Table.HeaderCell>
                      <Table.DataCell>{type}</Table.DataCell>
                      <Table.DataCell>{status}</Table.DataCell>
                      <Table.DataCell>{merknad}</Table.DataCell>
                      <Table.DataCell>{enhet}</Table.DataCell>
                      <Table.DataCell>
                        {saksbehandler && <RedigerSaksbehandler saksbehandler={saksbehandler} oppgaveId={id} />}
                      </Table.DataCell>
                      <Table.DataCell>{sakType ? sakType : 'Ingen saktype, må migreres'}</Table.DataCell>
                      <Table.DataCell>{frist ? frist : 'Ingen frist'}</Table.DataCell>
                    </Table.Row>
                  )
                )}
            </Table.Body>
          </Table>
        </div>
      ) : (
        <>Du har ingen oppgaver</>
      )}
    </>
  )
}
