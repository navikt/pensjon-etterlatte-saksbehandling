import { Alert, Button, Heading, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const GenerellOppgaveModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [displayError, setError] = useState(false)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [tilbakemeldingFraSaksbehandler, setTilbakemeldingFraSaksbehandler] = useState('')

  useEffect(() => {
    setError(false)
  }, [open])

  const avbryt = () => {
    if (tilbakemeldingFraSaksbehandler.length) {
      setError(false)
      const nyMerknad = `${oppgave.merknad}. Kommentar: ${tilbakemeldingFraSaksbehandler}`
      avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, () => {
        setOpen(false)
        oppgave.merknad = nyMerknad
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
        navigate(`/person/${oppgave.fnr}`)
      })
    } else {
      setError(true)
    }
  }

  const kanRedigeres = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" width="medium" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Generell oppgave
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <VStack gap="4">
            <Alert variant="info">{oppgave.merknad}</Alert>
            {kanRedigeres ? (
              <Textarea
                label="Kommentar"
                onChange={(e) => setTilbakemeldingFraSaksbehandler(e.target.value)}
                error={!displayError ? false : 'Du må legge til kommentar'}
              />
            ) : (
              <p>Oppgaven kan kun behandles av saksbehandler.</p>
            )}

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={() => setOpen(false)} loading={isPending(ferdigstillOppgaveStatus)}>
                Lukk
              </Button>

              {kanRedigeres && (
                <Button variant="danger" onClick={avbryt} loading={isPending(ferdigstillOppgaveStatus)}>
                  Avslutt oppgave
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
