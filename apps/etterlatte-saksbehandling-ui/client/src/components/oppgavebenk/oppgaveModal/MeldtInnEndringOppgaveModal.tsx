import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentJournalpost } from '~shared/api/dokument'
import { Alert, BodyShort, Button, HStack, Modal, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { Link } from 'react-router-dom'

enum HandlingValgt {
  AVSLUTT_OPPGAVE,
  OPPRETT_REVURDERING,
  INGEN,
}

interface Props {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const MeldtInnEndringOppgaveModal = ({ oppgave, oppdaterStatus }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [open, setOpen] = useState<boolean>(false)
  const [handlingValgt, setHandlingValgt] = useState<HandlingValgt>(HandlingValgt.INGEN)

  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  useEffect(() => {
    if (open) hentJournalpostFetch(oppgave.referanse!)
  }, [open])

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal
        open={open}
        aria-labelledby="Meldt inn endring oppgave modal"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Melding om endring' }}
      >
        <Modal.Body>
          <VStack gap="4">
            {kanRedigeres &&
              (erTildeltSaksbehandler ? (
                <>
                  {mapResult(hentJournalpostResult, {
                    pending: <Spinner label="Henter journalpost..." />,
                    success: (journalpost) => (
                      <Alert variant="info">
                        <VStack gap="2">
                          <BodyShort>
                            Bruker har meldt inn en endring. Velg mellom å opprette en revurdering eller å avslutte
                            oppgaven dersom den ikke skal behandles
                          </BodyShort>
                          <Link
                            to={`/api/dokumenter/${journalpost.journalpostId}/${journalpost.dokumenter[0].dokumentInfoId}`}
                            target="_blank"
                          >
                            <Button as="a" icon={<ExternalLinkIcon aria-hidden />} size="small">
                              Vis (åpnes i ny fane)
                            </Button>
                          </Link>
                        </VStack>
                      </Alert>
                    ),
                  })}
                </>
              ) : (
                <Alert variant="warning">Du må tildele deg oppgaven for å endre den</Alert>
              ))}

            <HStack justify="space-between">
              <Button variant="tertiary" onClick={() => setOpen(false)}>
                Avbryt
              </Button>
              {kanRedigeres && erTildeltSaksbehandler && (
                <>
                  {handlingValgt === HandlingValgt.INGEN && (
                    <HStack gap="4">
                      <Button variant="secondary">Avslutt oppgave</Button>
                      <Button>Opprett revurdering</Button>
                    </HStack>
                  )}
                  {handlingValgt === HandlingValgt.AVSLUTT_OPPGAVE && (
                    <HStack gap="4">
                      <Button variant="secondary">Tilbake</Button>
                      <Button>Avslutt</Button>
                    </HStack>
                  )}
                </>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
