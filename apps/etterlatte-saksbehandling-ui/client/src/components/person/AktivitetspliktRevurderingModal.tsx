import { BodyLong, Button, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { ferdigstillOppgaveMedMerknad, hentOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Toast } from '~shared/alerts/Toast'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'

export const AktivitetspliktRevurderingModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const [visModal, setVisModal] = useState(false)
  const [erFerdigstilt, setErFerdigstilt] = useState(false)
  const [beskrivelse, setBeskrivelse] = useState<string>('')

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
      apiFerdigstillOppgave({ id: oppgave.id, merknad: beskrivelse }, () => {
        setVisModal(false)
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
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
        <Modal open={visModal} onClose={() => setVisModal(false)} header={{ heading: 'Vurdering av aktivitetsplikt' }}>
          <Modal.Body>
            <VStack gap="4">
              <BodyLong>
                Den etterlatte skal være i minst 50% aktivitet 6 måneder etter dødsfallet. Det ser ikke ut til å ha
                blitt registrert noe aktivitet over 50% eller unntak på denne brukeren.
              </BodyLong>
              <BodyLong>
                Sak {oppgave.sakId} har en åpen behandling som forhindret Gjenny fra å opprette en revurdering
                automatisk. Vurder om det er nødvendig å opprette en revurdering manuelt.
              </BodyLong>

              {!erFerdigstilt && (
                <div>
                  <Textarea
                    label="Beskrivelse"
                    value={beskrivelse}
                    onChange={(e) => setBeskrivelse(e.target.value)}
                    placeholder="Beskrivelse"
                  />
                  <br />
                  <PersonButtonLink
                    variant="primary"
                    size="small"
                    target="_blank"
                    rel="noreferrer noopener"
                    fnr={oppgave.fnr || '-'}
                    disabled={!oppgave.fnr}
                  >
                    Gå til sak
                  </PersonButtonLink>
                </div>
              )}
              {erFerdigstilt && (
                <>
                  <BodyLong>
                    <i>Oppgaven ble ferdigstilt av {oppgave.saksbehandler?.navn}</i>
                  </BodyLong>
                  <BodyLong>
                    <b>Merknad:</b> {oppgave.merknad}
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
