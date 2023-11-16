import { Alert, Button, Heading, Modal, Table } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { FlexRow } from '~shared/styled'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentJournalpost } from '~shared/api/dokument'
import Spinner from '~shared/Spinner'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoList } from '~components/behandling/soeknadsoversikt/styled'

export const JournalfoeringOppgaveModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [open, setOpen] = useState(false)
  const { sakType } = oppgave

  const [journalpostStatus, apiHentJournalpost] = useApiCall(hentJournalpost)

  const aapneOppgave = () => {
    setOpen(true)

    if (!oppgave.referanse) throw Error('Mangler referanse (journalpostId)')

    apiHentJournalpost(oppgave.referanse)
  }

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={aapneOppgave}>
        Se oppgave
      </Button>
      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Journalføringsoppgave
          </Heading>
          <FlexRow $spacing>
            <SaktypeTag sakType={sakType} />
            <OppgavetypeTag oppgavetype="JOURNALFOERING" />
          </FlexRow>

          {mapApiResult(
            journalpostStatus,
            <Spinner label="Henter journalpost ..." visible />,
            () => (
              <Alert variant="error">En feil oppsto</Alert>
            ),
            (journalpost) => (
              <div>
                <Heading size="medium" spacing>
                  Info
                </Heading>

                <InfoList>
                  <Info label="Journalpost ID" tekst={journalpost.journalpostId} wide />
                  <Info label="Tittel" tekst={journalpost.tittel} wide />
                  <Info label="Kanal" tekst={journalpost.kanal} wide />
                  <Info
                    label="Avsender/mottaker"
                    tekst={`${journalpost.avsenderMottaker?.navn} (${journalpost.avsenderMottaker?.id})`}
                    wide
                  />
                </InfoList>

                <br />

                <Heading size="medium" spacing>
                  Dokumenter
                </Heading>

                <Table>
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell>ID</Table.HeaderCell>
                      <Table.HeaderCell>Tittel</Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {journalpost.dokumenter.map((dokument) => (
                      <Table.Row key={`dokument-${dokument.dokumentInfoId}`}>
                        <Table.DataCell>{dokument.dokumentInfoId}</Table.DataCell>
                        <Table.DataCell>{dokument.tittel}</Table.DataCell>
                        {/* TODO: Knapp for visning av dokument */}
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table>
                {journalpost.tittel.includes('P8000') && (
                  <Button variant="primary" as="a" href={`/oppgave/${oppgave.id}`}>
                    Opprett behandling
                  </Button>
                )}
              </div>
            )
          )}

          <br />

          <FlexRow justify="right">
            <Button variant="tertiary" onClick={() => setOpen(false)}>
              Avbryt
            </Button>
            {/* TODO: Knapp/select for handling*/}
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}
