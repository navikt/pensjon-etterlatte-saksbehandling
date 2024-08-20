import { Alert, BodyShort, Button, Heading, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const GenerellOppgaveModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [open, setOpen] = useState<boolean>(false)
  const [error, setError] = useState<string>('')
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [tilbakemeldingFraSaksbehandler, setTilbakemeldingFraSaksbehandler] = useState<string>('')

  const avslutt = () => {
    if (!!tilbakemeldingFraSaksbehandler) {
      const nyMerknad = `${oppgave.merknad}. Kommentar: ${tilbakemeldingFraSaksbehandler}`
      avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, () => {
        setError('')
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
        setOpen(false)
      })
    } else {
      setError('Legge til kommentar')
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
                error={error}
              />
            ) : (
              <BodyShort>Oppgaven kan kun behandles av saksbehandler.</BodyShort>
            )}

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={() => setOpen(false)} loading={isPending(ferdigstillOppgaveStatus)}>
                Lukk
              </Button>

              {kanRedigeres && (
                <Button variant="danger" onClick={avslutt} loading={isPending(ferdigstillOppgaveStatus)}>
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
