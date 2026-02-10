import { useState } from 'react'
import { Alert, BodyLong, BodyShort, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useNavigate } from 'react-router-dom'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { opprettNyKlage } from '~shared/api/klage'
import { NyKlageRequestUtfylling } from '~shared/types/Klage'
import { Journalpost } from '~shared/types/Journalpost'
import { OppgaveDTO } from '~shared/types/oppgave'
import { SakTypeTag } from '~shared/tags/SakTypeTag'

interface ModalProps {
  oppgave: OppgaveDTO
  klageRequest: NyKlageRequestUtfylling
  journalpost: Journalpost
}

export default function FullfoerKlageModal({ oppgave, klageRequest, journalpost }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [opprettKlageStatus, opprettKlageRequest] = useApiCall(opprettNyKlage)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstill = () => {
    opprettKlageRequest(
      {
        ...klageRequest,
        sakId: oppgave.sakId,
        innsender: journalpost.avsenderMottaker.id,
        journalpostId: journalpost.journalpostId,
      },
      () => {
        apiFerdigstillOppgave(oppgave.id, () => {
          setTimeout(() => {
            navigate('/')
          }, 5000)
        })
      }
    )
  }

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)}>
        Ferdigstill
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Ferdigstill og opprett ny klage
          </Heading>
          <BodyShort spacing>
            <SakTypeTag sakType={oppgave.sakType} />
          </BodyShort>

          <BodyLong spacing>
            Når du ferdigstiller journalføringsoppgaven vil det bli opprettet en ny klagebehandling for brukeren og
            oppgaven ferdigstilles.
          </BodyLong>

          {isSuccess(opprettKlageStatus) && isSuccess(ferdigstillOppgaveStatus) ? (
            <Alert variant="success">
              Klage opprettet for bruker med fødselsnummer {oppgave.fnr}. Du blir straks sendt til oppgavebenken.
            </Alert>
          ) : (
            <HStack gap="space-4" justify="center">
              <Button
                variant="secondary"
                onClick={() => setOpen(false)}
                disabled={isPending(opprettKlageStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                onClick={ferdigstill}
                loading={isPending(opprettKlageStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Ferdigstill
              </Button>
            </HStack>
          )}
          {isFailure(opprettKlageStatus) && (
            <Modal.Footer>
              <Alert variant="error">
                Det oppsto en feil ved oppretting av klagebehandlingen: {opprettKlageStatus.error.detail}
              </Alert>
            </Modal.Footer>
          )}
          {isFailure(ferdigstillOppgaveStatus) && (
            <Modal.Footer>
              <Alert variant="error">
                Det oppsto en feil ved lukking av oppgaven: {ferdigstillOppgaveStatus.error.detail}
              </Alert>
            </Modal.Footer>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
