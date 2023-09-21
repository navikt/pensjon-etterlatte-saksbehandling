import { useState } from 'react'
import { Alert, BodyLong, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'
import { FlexRow } from '~shared/styled'

interface ModalProps {
  oppgave: OppgaveDTOny
  behandlingBehov: NyBehandlingRequest
}

export default function FullfoerOppgaveModal({ oppgave, behandlingBehov }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [status, opprettNyBehandling] = useApiCall(opprettBehandling)

  const ferdigstill = () => {
    opprettNyBehandling(
      {
        ...behandlingBehov,
        mottattDato: behandlingBehov!!.mottattDato!!.replace('Z', ''),
      },
      () => {
        setTimeout(() => {
          navigate(`/person/${oppgave.fnr}`)
        }, 5000)
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
            Når du ferdigstiller journalføringsoppgaven vil det bli opprettet en ny behandling for brukeren. Etter
            opprettelse må du selv avslutte oppgaven i Gosys og ferdigstille journalposten.
          </BodyLong>

          {isSuccess(status) ? (
            <Alert variant="success">
              Behandling opprettet for bruker med fødselsnummer {oppgave.fnr}. Du blir straks sendt til saksoversikten
              for denne brukeren.
            </Alert>
          ) : (
            <FlexRow justify="center">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(status)}>
                Avbryt
              </Button>
              <Button variant="primary" onClick={ferdigstill} loading={isPending(status)} disabled={isPending(status)}>
                Ferdigstill
              </Button>
            </FlexRow>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
