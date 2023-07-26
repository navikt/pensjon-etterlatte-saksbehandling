import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { useAppSelector } from '~store/Store'
import { Pagination, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { FristHandlinger } from '~components/nyoppgavebenk/minoppgaveliste/FristHandlinger'
import { useState } from 'react'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'

export const MinOppgaveliste = (props: { oppgaver: ReadonlyArray<OppgaveDTOny> }) => {
  const { oppgaver } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [page, setPage] = useState<number>(1)
  const mineOppgaver = oppgaver.filter((o) => o.saksbehandler === user.ident)
  const rowsPerPage = 20
  let paginerteOppgaver = mineOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  return (
    <>
      {paginerteOppgaver.length > 0 ? (
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
                <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {paginerteOppgaver &&
                paginerteOppgaver.map(
                  ({ id, status, enhet, type, saksbehandler, opprettet, merknad, sakType, fnr, frist, sakId, referanse }) => (
                    <Table.Row key={id}>
                      <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                      <Table.HeaderCell>{fnr}</Table.HeaderCell>
                      <Table.DataCell>
                        <OppgavetypeTag oppgavetype={type} />
                      </Table.DataCell>
                      <Table.DataCell>{status}</Table.DataCell>
                      <Table.DataCell>{merknad}</Table.DataCell>
                      <Table.DataCell>{enhet}</Table.DataCell>
                      <Table.DataCell>
                        {saksbehandler && (
                          <RedigerSaksbehandler saksbehandler={saksbehandler} oppgaveId={id} sakId={sakId} />
                        )}
                      </Table.DataCell>
                      <Table.DataCell>{sakType && <SaktypeTag sakType={sakType} />}</Table.DataCell>
                      <Table.DataCell>
                        <FristHandlinger frist={frist} oppgaveId={id} sakId={sakId} />
                      </Table.DataCell>
                      <Table.DataCell>
                        <HandlingerForOppgave
                          oppgavetype={type}
                          fnr={fnr}
                          saksbehandler={saksbehandler}
                          referanse={referanse}
                        />
                      </Table.DataCell>
                    </Table.Row>
                  )
                )}
            </Table.Body>
          </Table>
          <Pagination
            page={page}
            onPageChange={setPage}
            count={Math.ceil(mineOppgaver.length / rowsPerPage)}
            size="small"
          />
        </div>
      ) : (
        <>Du har ingen oppgaver</>
      )}
    </>
  )
}
