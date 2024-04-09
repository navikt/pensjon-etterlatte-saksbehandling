import { Journalpost } from '~shared/types/Journalpost'
import { Alert, Button, Detail, Heading, Link, Modal } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedReferanse, opprettOppgave, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { isFailure, isPending, isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { FlexRow } from '~shared/styled'
import { SakMedBehandlinger } from '~components/person/typer'
import { useNavigate } from 'react-router-dom'
import { erOppgaveRedigerbar, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

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
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [kanOppretteOppgave, setKanOppretteOppgave] = useState(false)

  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)
  const [hentOppgaverStatus, hentOppgaver] = useApiCall(hentOppgaverMedReferanse)
  const [tildelSaksbehandlerStatus, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  useEffect(() => {
    if (isOpen) {
      hentOppgaver(journalpost.journalpostId, (oppgaver) => {
        const finnesUbehandletOppgave = oppgaver.filter(({ status }) => erOppgaveRedigerbar(status))

        setKanOppretteOppgave(!finnesUbehandletOppgave.length)
      })
    }
  }, [isOpen])

  const opprettJournalfoeringsoppgave = () => {
    if (isSuccess(sakStatus)) {
      const oppgaveType = Oppgavetype.JOURNALFOERING

      apiOpprettOppgave(
        {
          sakId: sakStatus.data.sak.id,
          request: {
            oppgaveType,
            referanse: journalpost.journalpostId,
            merknad: 'Manuell redigering av journalpost',
            oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
          },
        },
        (oppgave) => {
          tildelSaksbehandler(
            {
              oppgaveId: oppgave.id,
              type: oppgaveType,
              nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon: null },
            },
            () => navigate(`/oppgave/${oppgave.id}`)
          )
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
              {!oppgaver.length && (
                <Alert variant="info">Fant ingen andre oppgaver tilknyttet denne journalposten</Alert>
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

        {isFailure(tildelSaksbehandlerStatus) && (
          <Alert variant="error">
            Oppgaven ble opprettet, men tildeling feilet. Gå til oppgavelisten for å tildele den manuelt
          </Alert>
        )}

        <Modal.Footer>
          <FlexRow justify="right">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Avbryt
            </Button>

            <Button
              onClick={opprettJournalfoeringsoppgave}
              disabled={!kanOppretteOppgave || !isSuccess(sakStatus)}
              loading={isPending(opprettOppgaveStatus) || isPending(tildelSaksbehandlerStatus)}
            >
              Opprett oppgave
            </Button>
          </FlexRow>
        </Modal.Footer>
      </Modal>
    </>
  )
}
