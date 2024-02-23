import { Detail, Heading, Table } from '@navikt/ds-react'
import { formaterJournalpostStatus, formaterJournalpostType, formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { Journalpost } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

import { mapApiResult, Result } from '~shared/api/apiUtils'
import { VisDokument } from '~components/person/dokumenter/VisDokument'

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
                  <>
                    <Table.Row key={i} shadeOnHover={false}>
                      <Table.DataCell>{dokument.journalpostId}</Table.DataCell>
                      <Table.DataCell>{dokument.tittel}</Table.DataCell>
                      <Table.DataCell>{dokument.avsenderMottaker.navn || 'Ukjent'}</Table.DataCell>
                      <Table.DataCell>{formaterStringDato(dokument.datoOpprettet)}</Table.DataCell>
                      <Table.DataCell>
                        {dokument?.sak ? `${dokument.sak.fagsaksystem}: ${dokument.sak.fagsakId || '-'}` : '-'}
                      </Table.DataCell>
                      <Table.DataCell>{formaterJournalpostStatus(dokument.journalstatus)}</Table.DataCell>
                      <Table.DataCell>{formaterJournalpostType(dokument.journalposttype)}</Table.DataCell>
                      <Table.DataCell>
                        <VisDokument dokument={dokument} />
                      </Table.DataCell>
                    </Table.Row>
                  </>
                ))}
              </>
            )
        )}
      </Table.Body>
    </Table>
  </>
)
