import { Detail, Heading, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import DokumentModal from './dokumentModal'
import Spinner from '~shared/Spinner'
import { mapApiResult, Result } from '~shared/hooks/useApiCall'
import { Journalpost } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

const colonner = ['ID', 'Tittel', 'Avsender/Mottaker', 'Dato', 'Sak', 'Status', 'Type', '']

export const Dokumentliste = ({ dokumenter }: { dokumenter: Result<Journalpost[]> }) => (
  <>
    <Heading size="medium">Dokumenter</Heading>

    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          {colonner.map((col) => (
            <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
          ))}
        </Table.Row>
      </Table.Header>

      <Table.Body>
        {mapApiResult(
          dokumenter,
          <Table.Row>
            <Table.DataCell colSpan={colonner.length}>
              <Spinner margin="0" visible label="Henter dokumenter" />
            </Table.DataCell>
          </Table.Row>,
          () => (
            <Table.Row>
              <Table.DataCell colSpan={colonner.length}>
                <ApiErrorAlert>Det har oppstått en feil ved henting av dokumenter</ApiErrorAlert>
              </Table.DataCell>
            </Table.Row>
          ),
          (dokumentListe) =>
            !dokumentListe.length ? (
              <Table.Row shadeOnHover={false}>
                <Table.DataCell colSpan={colonner.length}>
                  <Detail>
                    <i>Ingen dokumenter funnet</i>
                  </Detail>
                </Table.DataCell>
              </Table.Row>
            ) : (
              <>
                {dokumentListe.map((dokument, i) => (
                  <Table.Row key={i} shadeOnHover={false}>
                    <Table.DataCell>{dokument.journalpostId}</Table.DataCell>
                    <Table.DataCell>{dokument.tittel}</Table.DataCell>
                    <Table.DataCell>{dokument.avsenderMottaker.navn || 'Ukjent'}</Table.DataCell>
                    <Table.DataCell>{formaterStringDato(dokument.datoOpprettet)}</Table.DataCell>
                    <Table.DataCell>
                      {dokument?.sak ? `${dokument.sak.fagsaksystem}: ${dokument.sak.fagsakId || '-'}` : '-'}
                    </Table.DataCell>
                    <Table.DataCell>{dokument.journalstatus}</Table.DataCell>
                    <Table.DataCell>{dokument.journalposttype === 'I' ? 'Inngående' : 'Utgående'}</Table.DataCell>
                    <Table.DataCell>
                      <DokumentModal
                        tittel={dokument.tittel}
                        journalpostId={dokument.journalpostId}
                        dokumentInfoId={dokument.dokumenter[0].dokumentInfoId}
                      />
                    </Table.DataCell>
                  </Table.Row>
                ))}
              </>
            )
        )}
      </Table.Body>
    </Table>
  </>
)
