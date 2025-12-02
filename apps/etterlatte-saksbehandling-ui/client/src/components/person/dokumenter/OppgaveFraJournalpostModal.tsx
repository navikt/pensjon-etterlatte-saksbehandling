import { Journalpost } from '~shared/types/Journalpost'
import { Alert, Button, Detail, Heading, HStack, Link, Modal, VStack } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedReferanse, opprettOppgave } from '~shared/api/oppgaver'
import { isPending, isSuccess, mapFailure, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { SakMedBehandlinger } from '~components/person/typer'
import { useNavigate } from 'react-router-dom'
import { erOppgaveRedigerbar, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ConfigContext } from '~clientConfig'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { ApiErrorAlert } from '~ErrorBoundary'
import { flyttTilGjenny, hentJournalfoeringsoppgaverFraGosys } from '~shared/api/gosys'
import { GosysOppgave } from '~shared/types/Gosys'
import { ClickEvent, trackClick } from '~utils/analytics'

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
  const configContext = useContext(ConfigContext)

  const [kanOppretteOppgave, setKanOppretteOppgave] = useState(false)
  const [finnesGosysOppgave, setFinnesGosysOppgave] = useState(false)

  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)
  const [hentOppgaverStatus, hentOppgaver] = useApiCall(hentOppgaverMedReferanse)

  const [gosysResult, hentGosysOppgave] = useApiCall(hentJournalfoeringsoppgaverFraGosys)
  const [flyttOppgaveResult, flyttOppgaveTilGjenny] = useApiCall(flyttTilGjenny)

  useEffect(() => {
    if (isOpen) {
      hentGosysOppgave(journalpost.journalpostId, (oppgaver) => {
        setFinnesGosysOppgave(!!oppgaver?.length)
      })

      hentOppgaver(journalpost.journalpostId, (oppgaver) => {
        const finnesUbehandletOppgave = oppgaver.filter(({ status }) => erOppgaveRedigerbar(status))

        setKanOppretteOppgave(!finnesUbehandletOppgave.length)
      })
    }
  }, [isOpen])

  const opprettJournalfoeringsoppgave = () => {
    if (isSuccess(sakStatus)) {
      trackClick(ClickEvent.OPPRETT_JOURNALFOERINGSOPPGAVE)

      apiOpprettOppgave(
        {
          sakId: sakStatus.data.sak.id,
          request: {
            oppgaveType: Oppgavetype.JOURNALFOERING,
            referanse: journalpost.journalpostId,
            merknad: 'Manuell redigering av journalpost',
            oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
            saksbehandler: innloggetSaksbehandler.ident,
          },
        },
        (oppgave) => {
          navigate(`/oppgave/${oppgave.id}`)
        }
      )
    }
  }

  const konverterTilGjennyoppgave = (oppgave: GosysOppgave) => {
    if (isSuccess(sakStatus)) {
      trackClick(ClickEvent.FLYTT_GOSYS_OPPGAVE)

      flyttOppgaveTilGjenny(
        { oppgaveId: oppgave.id, sakId: sakStatus.data.sak.id, enhetsnr: sakStatus.data.sak.enhet },
        (oppgave) => {
          navigate(`/oppgave/${oppgave.id}`)
        }
      )
    }
  }

  return (
    <>
      <Button variant="secondary" size="small" icon={<PencilIcon title="Rediger" />} onClick={() => setIsOpen(true)} />

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

        <Modal.Body>
          {mapResult(gosysResult, {
            pending: <Spinner label="Sjekker om det finnes Gosys-oppgaver tilknyttet journalposten" />,
            error: (error) => (
              <ApiErrorAlert>{error.detail || 'Feil oppsto ved henting av oppgaver fra Gosys'}</ApiErrorAlert>
            ),
            success: (oppgaver) =>
              finnesGosysOppgave ? (
                <VStack gap="2">
                  <Alert variant="warning">
                    Fant {oppgaver.length} oppgave(r) tilknyttet journalposten i Gosys.
                    <br />
                    <Link
                      href={`${configContext['gosysUrl']}/personoversikt/fnr=${oppgaver[0].bruker?.ident}`}
                      target="_blank"
                    >
                      Åpne i Gosys <ExternalLinkIcon aria-hidden />
                    </Link>
                  </Alert>

                  <br />

                  {oppgaver.map((oppgave) => (
                    <div key={oppgave.id}>
                      <VStack gap="4">
                        <Info label="ID" tekst={oppgave.id} />
                        <Info label="Beskrivelse" tekst={oppgave.beskrivelse} />
                      </VStack>
                      <br />

                      <HStack gap="4" justify="end">
                        {isSuccess(flyttOppgaveResult) ? (
                          <Alert size="small" variant="success">
                            <Link href={`/oppgave/${flyttOppgaveResult.data.id}`}>Gå til oppgave</Link>
                          </Alert>
                        ) : (
                          <Button
                            size="small"
                            variant="secondary"
                            onClick={() => konverterTilGjennyoppgave(oppgave)}
                            loading={isPending(flyttOppgaveResult)}
                          >
                            Flytt til Gjenny
                          </Button>
                        )}
                      </HStack>
                    </div>
                  ))}
                </VStack>
              ) : null,
          })}

          {mapResult(hentOppgaverStatus, {
            pending: <Spinner label="Sjekker om det allerede finnes en oppgave" />,
            success: () =>
              kanOppretteOppgave ? (
                isSuccess(sakStatus) ? (
                  finnesGosysOppgave ? null : (
                    <Alert variant="info">Ny journalføringsoppgave kan opprettes</Alert>
                  )
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
                    Gå til oppgavelisten <ExternalLinkIcon aria-hidden />
                  </Link>
                </Alert>
              ),
          })}
        </Modal.Body>

        <Modal.Footer>
          {mapFailure(flyttOppgaveResult, (error) => (
            <ApiErrorAlert>{error.detail || 'Ukjent feil oppsto ved flytting av oppgave'}</ApiErrorAlert>
          ))}

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Avbryt
            </Button>

            <Button
              onClick={opprettJournalfoeringsoppgave}
              disabled={!kanOppretteOppgave || finnesGosysOppgave || !isSuccess(sakStatus)}
              loading={isPending(opprettOppgaveStatus)}
            >
              Opprett oppgave
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}
