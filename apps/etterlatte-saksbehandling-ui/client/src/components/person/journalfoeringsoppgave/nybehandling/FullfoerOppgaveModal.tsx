import { useState } from 'react'
import { Alert, BodyLong, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { SaktypeTag } from '~components/oppgavebenk/components/Tags'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'
import { FlexRow } from '~shared/styled'

import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { OppgaveDTO } from '~shared/types/oppgave'

interface ModalProps {
  oppgave: OppgaveDTO
  behandlingBehov: NyBehandlingRequest
}

export default function FullfoerOppgaveModal({ oppgave, behandlingBehov }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [opprettBehandlingStatus, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstill = () => {
    opprettNyBehandling(
      {
        ...behandlingBehov,
        mottattDato: behandlingBehov!!.mottattDato!!.replace('Z', ''),
      },
      () => {
        apiFerdigstillOppgave(oppgave.id)
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
            Ferdigstill og opprett ny behandling
          </Heading>
          <BodyShort spacing>
            <SaktypeTag sakType={oppgave.sakType} />
          </BodyShort>

          <BodyLong spacing>
            Når du ferdigstiller journalføringsoppgaven vil det bli opprettet en ny behandling for brukeren og oppgaven
            ferdigstilles.
          </BodyLong>

          {isSuccess(opprettBehandlingStatus) && isSuccess(ferdigstillOppgaveStatus) ? (
            <>
              <Alert variant="success">Behandling opprettet for bruker med fødselsnummer {oppgave.fnr}</Alert>

              <br />

              <FlexRow justify="center">
                <Button variant="secondary" onClick={() => navigate('/')}>
                  Gå til oppgavelisten
                </Button>
                <Button variant="primary" onClick={() => navigate(`/person/${oppgave.fnr}`)}>
                  Gå til sakoversikten
                </Button>
              </FlexRow>
            </>
          ) : (
            <FlexRow justify="center">
              <Button
                variant="secondary"
                onClick={() => setOpen(false)}
                disabled={isPending(opprettBehandlingStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                onClick={ferdigstill}
                loading={isPending(opprettBehandlingStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Ferdigstill
              </Button>
            </FlexRow>
          )}
          {mapFailure(opprettBehandlingStatus, (error) => (
            <Modal.Footer>
              <Alert variant="error">{error.detail || 'Det oppsto en feil ved oppretting av behandlingen.'}</Alert>
            </Modal.Footer>
          ))}
          {mapFailure(ferdigstillOppgaveStatus, (error) => (
            <Modal.Footer>
              <Alert variant="error">{error.detail || 'Det oppsto en feil ved lukking av oppgaven'}</Alert>
            </Modal.Footer>
          ))}
        </Modal.Body>
      </Modal>
    </>
  )
}
