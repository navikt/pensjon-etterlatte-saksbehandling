import { Journalpost } from '~shared/types/Journalpost'
import { Alert, Button, Detail, Heading, Link, Modal } from '@navikt/ds-react'
import React, { useContext, useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  feilregistrerGosysOppgave,
  hentJournalfoeringsoppgaverFraGosys,
  hentOppgaverMedReferanse,
  opprettOppgave,
  tildelSaksbehandlerApi,
} from '~shared/api/oppgaver'
import { isFailure, isPending, isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { FlexRow } from '~shared/styled'
import { SakMedBehandlinger } from '~components/person/typer'
import { useNavigate } from 'react-router-dom'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ConfigContext } from '~clientConfig'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { ApiErrorAlert } from '~ErrorBoundary'

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
  const [tildelSaksbehandlerStatus, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const [gosysResult, hentGosysOppgave] = useApiCall(hentJournalfoeringsoppgaverFraGosys)
  const [, feilregistrerOppgave] = useApiCall(feilregistrerGosysOppgave)

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

  const konverterTilGjennyoppgave = (oppgave: OppgaveDTO) => {
    if (isSuccess(sakStatus)) {
      apiOpprettOppgave(
        {
          sakId: sakStatus.data.sak.id,
          request: {
            oppgaveType: Oppgavetype.JOURNALFOERING,
            referanse: oppgave.journalpostId!!,
            merknad: oppgave.beskrivelse || 'Journalføringsoppgave flyttet fra Gosys',
            oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
          },
        },
        (opprettetOppgave) => {
          tildelSaksbehandler({
            oppgaveId: opprettetOppgave.id,
            type: Oppgavetype.JOURNALFOERING,
            nysaksbehandler: {
              saksbehandler: innloggetSaksbehandler.ident,
              versjon: null,
            },
          })

          feilregistrerOppgave({
            oppgaveId: oppgave.id,
            versjon: oppgave.versjon!!,
            beskrivelse: 'Oppgave ble flyttet til Gjenny',
          })
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

        <Modal.Body>
          {mapResult(gosysResult, {
            pending: <Spinner label="Sjekker om det finnes Gosys-oppgaver tilknyttet journalposten" visible />,
            error: (error) => (
              <ApiErrorAlert>{error.detail || 'Feil oppsto ved henting av oppgaver fra Gosys'}</ApiErrorAlert>
            ),
            success: (oppgaver) =>
              finnesGosysOppgave ? (
                <>
                  <Alert variant="warning">
                    Fant {oppgaver.length} oppgave(r) tilknyttet journalposten i Gosys.
                    <br />
                    <Link href={`${configContext['gosysUrl']}/personoversikt/fnr=${oppgaver[0].fnr}`} target="_blank">
                      Åpne i Gosys <ExternalLinkIcon />
                    </Link>
                  </Alert>

                  <br />

                  {oppgaver.map((oppgave) => (
                    <div key={oppgave.id}>
                      <InfoWrapper>
                        <Info label="ID" tekst={oppgave.id} />
                        <Info label="Beskrivelse" tekst={oppgave.beskrivelse} />
                      </InfoWrapper>
                      <br />

                      <FlexRow $spacing justify="right">
                        {isSuccess(tildelSaksbehandlerStatus) && isSuccess(opprettOppgaveStatus) ? (
                          <Alert size="small" variant="success">
                            <Link href={`/oppgave/${opprettOppgaveStatus.data.id}`}>Gå til oppgave</Link>
                          </Alert>
                        ) : (
                          <Button
                            size="small"
                            variant="secondary"
                            onClick={() => konverterTilGjennyoppgave(oppgave)}
                            loading={isPending(opprettOppgaveStatus) || isPending(tildelSaksbehandlerStatus)}
                          >
                            Flytt til Gjenny
                          </Button>
                        )}
                      </FlexRow>
                    </div>
                  ))}
                </>
              ) : null,
          })}

          {mapResult(hentOppgaverStatus, {
            pending: <Spinner visible label="Sjekker om det allerede finnes en oppgave" />,
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
                    Gå til oppgavelisten <ExternalLinkIcon />
                  </Link>
                </Alert>
              ),
          })}
        </Modal.Body>

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
              disabled={!kanOppretteOppgave || finnesGosysOppgave || !isSuccess(sakStatus)}
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
