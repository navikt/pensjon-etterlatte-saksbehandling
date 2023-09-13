import { Alert, Detail, Heading, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import DokumentModal from './dokumentModal'
import Spinner from '~shared/Spinner'
import { isFailure, isPending, isSuccess, Result } from '~shared/hooks/useApiCall'
import { Journalpost } from '~shared/types/Journalpost'

const colonner = ['Journalpost id', 'Tittel', 'Avsender/Mottaker', 'Dato', 'Status', 'Type', '']

export const Dokumentliste = ({ dokumenter }: { dokumenter: Result<Journalpost[]> }) => (
  <>
    <Heading size={'medium'}>Dokumenter</Heading>

    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          {colonner.map((col) => (
            <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {isPending(dokumenter) && (
          <Table.Row>
            <Table.DataCell colSpan={colonner.length}>
              <Spinner margin={'0'} visible label="Henter dokumenter" />
            </Table.DataCell>
          </Table.Row>
        )}

        {isSuccess(dokumenter) &&
          (!dokumenter.data.length ? (
            <Table.Row shadeOnHover={false}>
              <Table.DataCell colSpan={colonner.length}>
                <Detail>
                  <i>Ingen dokumenter funnet</i>
                </Detail>
              </Table.DataCell>
            </Table.Row>
          ) : (
            dokumenter.data.map((brev, i) => (
              <Table.Row key={i} shadeOnHover={false}>
                <Table.DataCell key={`data${brev.journalpostId}`}>{brev.journalpostId}</Table.DataCell>
                <Table.DataCell key={`data${brev.tittel}`}>{brev.tittel}</Table.DataCell>
                <Table.DataCell key={`data${brev.avsenderMottaker.navn}`}>
                  {brev.avsenderMottaker.navn || 'Ukjent'}
                </Table.DataCell>
                <Table.DataCell key={`data${brev.datoOpprettet}`}>
                  {formaterStringDato(brev.datoOpprettet)}
                </Table.DataCell>
                <Table.DataCell key={`data${brev.journalstatus}`}>{brev.journalstatus}</Table.DataCell>
                <Table.DataCell key={`data${brev.journalposttype}`}>
                  {brev.journalposttype === 'I' ? 'Inngående' : 'Utgående'}
                </Table.DataCell>
                <Table.DataCell>
                  <DokumentModal
                    tittel={brev.tittel}
                    journalpostId={brev.journalpostId}
                    dokumentInfoId={brev.dokumenter[0].dokumentInfoId}
                  />
                </Table.DataCell>
              </Table.Row>
            ))
          ))}
      </Table.Body>
    </Table>

    {isFailure(dokumenter) && (
      <Alert variant={'error'} style={{ marginTop: '10px' }}>
        Det har oppstått en feil ved henting av dokumenter.
      </Alert>
    )}
  </>
)
