import { Button, Detail, Heading, Modal, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import DokumentModal from './dokumentModal'
import Spinner from '~shared/Spinner'
import { Journalpost } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

import { mapApiResult, Result } from '~shared/api/apiUtils'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { FlexRow } from '~shared/styled'
import { useState } from 'react'

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
                      <FlexRow justify="right">
                        <UtsendingsinfoModal journalpost={dokument} />

                        <DokumentModal
                          tittel={dokument.tittel}
                          journalpostId={dokument.journalpostId}
                          dokumentInfoId={dokument.dokumenter[0].dokumentInfoId}
                        />
                      </FlexRow>
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

const UtsendingsinfoModal = ({ journalpost }: { journalpost: Journalpost }) => {
  const [isOpen, setIsOpen] = useState(false)

  if (journalpost.journalposttype !== 'U' || !journalpost.utsendingsinfo) return null

  return (
    <>
      <Button
        variant="tertiary"
        title="Utsendingsinfo"
        size="small"
        icon={<InformationSquareIcon />}
        onClick={() => setIsOpen(true)}
      />

      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Header>
          <Heading size="medium">Utsendingsinfo</Heading>
        </Modal.Header>

        <Modal.Body>
          <pre>{JSON.stringify(journalpost.utsendingsinfo, null, 2)}</pre>
        </Modal.Body>
      </Modal>
    </>
  )
}
