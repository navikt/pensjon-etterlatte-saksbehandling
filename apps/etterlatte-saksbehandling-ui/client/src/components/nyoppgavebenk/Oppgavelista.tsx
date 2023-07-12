import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentNyeOppgaver, OppgaveDTOny } from '~shared/api/oppgaverny'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { Table } from '@navikt/ds-react'

export const Oppgavelista = () => {
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [hentedeOppgaver, setHentedeOppgaver] = useState<ReadonlyArray<OppgaveDTOny> | undefined>()
  useEffect(() => {
    hentOppgaver(
      {},
      (oppgaver) => {
        setHentedeOppgaver(oppgaver)
      },
      () => {
        console.error('kunne ikke hente oppgaver')
      }
    )
  }, [])
  console.log(hentedeOppgaver)
  return (
    <div>
      {isPending(oppgaver) && <Spinner visible={true} label={'henter nye oppgaver'} />}
      {isSuccess(oppgaver) && <>hentet antall oppgaver: {hentedeOppgaver?.length}</>}
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
            <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
            <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {hentedeOppgaver &&
            hentedeOppgaver.map(({ id, status, enhet, type, saksbehandler, opprettet, merknad }) => (
              <Table.Row key={id}>
                <Table.HeaderCell>{opprettet}</Table.HeaderCell>
                <Table.DataCell>{type}</Table.DataCell>
                <Table.DataCell>{status}</Table.DataCell>
                <Table.DataCell>{merknad}</Table.DataCell>
                <Table.DataCell>{enhet}</Table.DataCell>
                <Table.DataCell>{saksbehandler ? saksbehandler : 'Tildel meg'}</Table.DataCell>
              </Table.Row>
            ))}
        </Table.Body>
      </Table>
    </div>
  )
}
