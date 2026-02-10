import { BodyLong, Button, Heading, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { ferdigstillOppgaveMedMerknad, hentOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Toast } from '~shared/alerts/Toast'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { EnvelopeClosedIcon } from '@navikt/aksel-icons'
import { PersonOversiktFane } from '~components/person/Person'

export const AktivitetspliktInfo6MndVarigUnntakModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const [visModal, setVisModal] = useState(false)
  const [erFerdigstilt, setErFerdigstilt] = useState(false)
  const [begrunnelse, setBegrunnelse] = useState<string>('')

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [hentOppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)

  useEffect(() => {
    if (visModal) {
      apiHentOppgave(oppgave.id, (result) => {
        setErFerdigstilt(result.status === Oppgavestatus.FERDIGSTILT)
      })
    }
  }, [visModal])

  const ferdigstill = () => {
    if (!erFerdigstilt) {
      const merknad = begrunnelse ? begrunnelse : oppgave.merknad
      apiFerdigstillOppgave({ id: oppgave.id, merknad }, (oppgave) => {
        setVisModal(false)
        oppdaterStatus(oppgave.id, oppgave.status)
      })
    }
  }

  return (
    <>
      {isSuccess(ferdigstillOppgaveStatus) && (
        <Toast
          timeout={10000}
          melding="Oppgave ferdigstilt, har du husket å ferdigstille pågående behandling og opprette ny revurdering?"
        />
      )}
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal
          open={visModal}
          onClose={() => setVisModal(false)}
          header={{ heading: 'Utsendelse av brev ved 6 måneder ved varig unntak' }}
        >
          <Modal.Body>
            <VStack gap="space-4">
              <div>
                <Heading size="xsmall">Opprett informasjonsbrev til bruker</Heading>
                <BodyLong>
                  Den etterlatte skal informeres om stønaden sin og hvilke krav som stilles til dem i tiden
                  fremover.{' '}
                </BodyLong>
              </div>
              <BodyLong>
                Opprett brev som skal sendes ved 6 måneder etter dødsfall. Etter brevet er opprettet og sendt må du
                ferdigstille oppgaven her.
              </BodyLong>

              <div>
                <PersonButtonLink
                  size="small"
                  icon={<EnvelopeClosedIcon aria-hidden />}
                  fnr={oppgave.fnr || '-'}
                  fane={PersonOversiktFane.BREV}
                  disabled={!oppgave.fnr}
                >
                  Gå til brevoversikt
                </PersonButtonLink>
              </div>

              {!erFerdigstilt && (
                <>
                  <Heading size="small">Ferdigstill oppgave</Heading>
                  <Textarea
                    label="Beskrivelse (valgfritt)"
                    value={begrunnelse}
                    onChange={(e) => setBegrunnelse(e.target.value)}
                  />
                </>
              )}
              {erFerdigstilt && (
                <>
                  <BodyLong>
                    <em>Oppgaven ble ferdigstilt av {oppgave.saksbehandler?.navn}</em>
                  </BodyLong>
                  <BodyLong>
                    <strong>Merknad:</strong> {oppgave.merknad}
                  </BodyLong>
                </>
              )}
            </VStack>
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgave'}</ApiErrorAlert>
            ))}
            {mapFailure(hentOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved henting av oppgave'}</ApiErrorAlert>
            ))}
          </Modal.Body>
          <Modal.Footer>
            {!erFerdigstilt && (
              <Button
                loading={isPending(ferdigstillOppgaveStatus) || isPending(hentOppgaveStatus)}
                variant="primary"
                type="button"
                onClick={ferdigstill}
              >
                Ferdigstill oppgave
              </Button>
            )}
            <Button loading={isPending(ferdigstillOppgaveStatus)} variant="tertiary" onClick={() => setVisModal(false)}>
              Avbryt
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  )
}
