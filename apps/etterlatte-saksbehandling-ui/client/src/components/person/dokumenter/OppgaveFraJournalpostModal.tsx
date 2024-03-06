import { Journalpost } from '~shared/types/Journalpost'
import { Accordion, Alert, BodyShort, Button, Detail, Heading, Link, Modal } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedReferanse, opprettOppgave } from '~shared/api/oppgaver'
import { isPending, isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { OppgavetypeTag } from '~components/oppgavebenk/components/Tags'
import { FlexRow } from '~shared/styled'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterOppgaveStatus, formaterSakstype } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { useNavigate } from 'react-router-dom'

// TODO: Må på sikt gjøre noe for å støtte tilfeller hvor sak mangler.
export const OppgaveFraJournalpostModal = ({
  isOpen,
  setIsOpen,
  journalpost,
  sakStatus,
}: {
  isOpen: boolean
  setIsOpen: (isOpen: boolean) => void
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
}) => {
  const navigate = useNavigate()

  const [kanOppretteOppgave, setKanOppretteOppgave] = useState(false)

  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)
  const [hentOppgaverStatus, hentOppgaver] = useApiCall(hentOppgaverMedReferanse)

  useEffect(() => {
    if (isOpen) {
      hentOppgaver(journalpost.journalpostId, (oppgaver) => {
        const finnesUbehandletOppgave = oppgaver.filter(
          ({ status }) => !['FERDIGSTILT', 'FEILREGISTRERT', 'AVBRUTT'].includes(status)
        )

        setKanOppretteOppgave(!finnesUbehandletOppgave.length)
      })
    }
  }, [isOpen])

  const opprettJournalfoeringsoppgave = () => {
    console.log('oppretter oppgave')

    if (isSuccess(sakStatus)) {
      apiOpprettOppgave(
        {
          sakId: sakStatus.data.sak.id,
          request: {
            oppgaveType: 'JOURNALFOERING',
            referanse: journalpost.journalpostId,
            merknad: 'Manuell redigering av journalpost',
            oppgaveKilde: 'HENDELSE', // TODO: Vi burde kanskje ha egen kilde for oppgaver saksbehandler selv oppretter?
          },
        },
        (result) => {
          navigate(`/oppgave/${result.id}`)
        }
      )
    }
  }

  return (
    <>
      <Button variant="secondary" size="small" icon={<PencilIcon />} onClick={() => setIsOpen(true)} title="Rediger" />

      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
        }}
        aria-labelledby="modal-heading"
        width="medium"
      >
        <Modal.Header>
          <Heading size="medium">Opprett journalføringsoppgave</Heading>
          <Detail>Journalpost {journalpost.journalpostId}</Detail>
        </Modal.Header>

        {mapResult(hentOppgaverStatus, {
          pending: <Spinner visible label="Sjekker om det allerede finnes en oppgave" />,
          success: (oppgaver) => (
            <Modal.Body>
              {!oppgaver.length ? (
                <Alert variant="info">Fant ingen andre oppgaver tilknyttet denne journalposten</Alert>
              ) : (
                <>
                  <BodyShort spacing>Fant oppgave(r) tilknyttet journalposten:</BodyShort>

                  {oppgaver.map((oppgave) => (
                    <Accordion key={oppgave.id}>
                      <Accordion.Item>
                        <Accordion.Header>
                          <OppgavetypeTag oppgavetype={oppgave.type} />
                        </Accordion.Header>
                        <Accordion.Content>
                          <InfoWrapper>
                            <Info label="Status" tekst={formaterOppgaveStatus(oppgave.status)} />
                            <Info label="Saksbehandler" tekst={oppgave.saksbehandler?.navn || '-'} />
                            <Info label="Saktype" tekst={formaterSakstype(oppgave.sakType)} />
                            <Info label="Merknad" tekst={oppgave.merknad} />
                          </InfoWrapper>
                        </Accordion.Content>
                      </Accordion.Item>
                    </Accordion>
                  ))}
                </>
              )}

              <br />

              {kanOppretteOppgave ? (
                isSuccess(sakStatus) ? (
                  <Alert variant="info">Ny journalføringsoppgave kan opprettes</Alert>
                ) : (
                  <Alert variant="warning">
                    Det finnes ingen sak på denne brukeren. Kan ikke opprette oppgave uten sak.
                  </Alert>
                )
              ) : (
                <Alert variant="warning">
                  Det finnes allerede en ubehandlet journalføringsoppgave tilknyttet journalpost{' '}
                  {journalpost.journalpostId}. Du må ferdigstille den eksisterende oppgaven før du kan opprette en ny.
                  <br />
                  <Link href="/" target="_blank">
                    Gå til oppgavelisten <ExternalLinkIcon />
                  </Link>
                </Alert>
              )}
            </Modal.Body>
          ),
        })}

        <Modal.Footer>
          <FlexRow justify="right">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Avbryt
            </Button>

            <Button
              onClick={opprettJournalfoeringsoppgave}
              disabled={!kanOppretteOppgave}
              loading={isPending(opprettOppgaveStatus)}
            >
              Opprett oppgave
            </Button>
          </FlexRow>
        </Modal.Footer>
      </Modal>
    </>
  )
}
