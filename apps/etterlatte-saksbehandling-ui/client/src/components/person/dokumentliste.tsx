import { Alert, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import { Journalpost } from '../behandling/types'
import { formaterDato } from '../../utils/formattering'
import InnkommendeBrevModal from '../behandling/brev/innkommende-brev-modal'
import Spinner from '../../shared/Spinner'

const colonner = ['Journalpost id', 'Tittel', 'Avsender', 'Dato', 'Status', 'Type', '']

export const Dokumentliste = ({
  dokumenter,
  dokumenterHentet,
  error,
}: {
  dokumenter: Journalpost[]
  dokumenterHentet: boolean
  error: boolean
}) => {
  return (
    <TableWrapper>
      <Table>
        <Table.Header className="test">
          <Table.Row>
            {colonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        {dokumenter.map((brev, i) => (
          <Table.Body key={i}>
            <Table.Row>
              <Table.DataCell key={`data${brev.journalpostId}`}>{brev.journalpostId}</Table.DataCell>
              <Table.DataCell key={`data${brev.tittel}`}>{brev.tittel}</Table.DataCell>
              <Table.DataCell key={`data${brev.avsenderMottaker.navn}`}>{brev.avsenderMottaker.navn}</Table.DataCell>
              <Table.DataCell key={`data${brev.datoOpprettet}`}>
                {formaterDato(new Date(brev.datoOpprettet))}
              </Table.DataCell>
              <Table.DataCell key={`data${brev.journalstatus}`}>{brev.journalstatus}</Table.DataCell>
              <Table.DataCell key={`data${brev.journalposttype}`}>
                {brev.journalposttype === 'I' ? 'Inngående' : 'Utgående'}
              </Table.DataCell>
              <Table.DataCell>
                <InnkommendeBrevModal
                  tittel={brev.tittel}
                  journalpostId={brev.journalpostId}
                  dokumentInfoId={brev.dokumenter[0].dokumentInfoId}
                />
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        ))}
        {dokumenter.length === 0 && !error && (
          <Table.Body>
            <Table.Row>
              <IngenInnkommendeBrevRad colSpan={6}>
                {dokumenterHentet ? (
                  'Ingen dokumenter ble funnet'
                ) : (
                  <Spinner margin={'0'} visible={!dokumenterHentet} label="Henter innkommende brev" />
                )}
              </IngenInnkommendeBrevRad>
            </Table.Row>
          </Table.Body>
        )}
      </Table>
      {error && (
        <Alert variant={'error'} style={{ marginTop: '10px' }}>
          Det har oppstått en feil ved henting av henting av dokumenter..
        </Alert>
      )}
    </TableWrapper>
  )
}

export const TableWrapper = styled.div`
  .test {
    border-bottom: 1.5px solid #000000;
  }
`

const IngenInnkommendeBrevRad = styled.td`
  text-align: center;
  padding-top: 16px;
  font-style: italic;
`
