import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import React, { useEffect, useState } from 'react'
import { Alert, BodyShort, Button, Heading, HStack, Label, Link, List, Modal, Textarea, VStack } from '@navikt/ds-react'
import { CalendarIcon, EyeIcon, FilePlusIcon } from '@navikt/aksel-icons'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { formaterDato } from '~utils/formatering/dato'
import { hentJournalpost } from '~shared/api/dokument'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { PersonLink } from '~components/person/lenker/PersonLink'

interface Props {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const InntektsopplysningModal = ({ oppgave, oppdaterStatus }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [open, setOpen] = useState<boolean>(false)

  const [beskrivelse, setBeskrivelse] = useState<string>('')

  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)
  const [ferdigstillOppgaveResult, ferdigstillOppgaveFunc] = useApiCall(ferdigstillOppgaveMedMerknad)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const ferdigstillOppgave = () => {
    const nyMerknad = !!beskrivelse ? `Kommentar: ${beskrivelse}` : ''

    ferdigstillOppgaveFunc({ id: oppgave.id, merknad: nyMerknad }, () => {
      oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
      setOpen(false)
    })
  }

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
        aria-labelledby="modal-heading"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Ny inntektsopplysning' }}
      >
        <Modal.Body>
          <VStack gap="space-4">
            <HStack gap="space-8">
              <VStack gap="space-2">
                <HStack gap="space-1" align="center">
                  <FilePlusIcon fontSize="1.5rem" /> <Label>Opprettet</Label>
                </HStack>
                <BodyShort>{formaterDato(oppgave.opprettet)}</BodyShort>
              </VStack>
              <VStack gap="space-2">
                <HStack gap="space-1" align="center">
                  <CalendarIcon fontSize="1.5rem" /> <Label>Frist</Label>
                </HStack>
                <BodyShort>{formaterDato(oppgave.frist)}</BodyShort>
              </VStack>
            </HStack>

            {kanRedigeres &&
              (erTildeltSaksbehandler ? (
                <>
                  {mapResult(hentJournalpostResult, {
                    pending: <Spinner label="Henter journalpost" />,
                    success: (journalpost) => (
                      <VStack gap="space-4">
                        <BodyShort>Mottatt inntektsopplysning for neste år</BodyShort>
                        <BodyShort size="small">For å behandle denne oppgaven, følg disse stegene:</BodyShort>
                        <List as="ol">
                          <List.Item>
                            <Link
                              href={`/api/dokumenter/${journalpost.journalpostId}/${journalpost.dokumenter[0].dokumentInfoId}`}
                              target="_blank"
                              inlineText
                            >
                              Gå til dokumentoversikten (åpnes i ny fane)
                            </Link>{' '}
                            for å se inntektsopplysningen.
                          </List.Item>
                          <List.Item>
                            Vurder om det skal opprettes en revurdering, eller om det er nok å sende informasjon til{' '}
                            {!!oppgave.fnr ? (
                              <PersonLink fnr={oppgave.fnr} target="_blank">
                                bruker
                              </PersonLink>
                            ) : (
                              'bruker'
                            )}
                            .
                          </List.Item>
                          <List.Item>
                            Ved revurdering, opprett en revurdering og velg årsak “endring av inntekt”.
                          </List.Item>
                          <List.Item>Ferdigstill denne oppgaven.</List.Item>
                        </List>

                        <VStack gap="space-2" maxWidth="20rem">
                          <Heading size="small">Ferdigstill oppgave</Heading>
                          <Textarea
                            label="Beskrivelse (valgfritt)"
                            value={beskrivelse}
                            onChange={(e) => setBeskrivelse(e.target.value)}
                          />
                        </VStack>
                      </VStack>
                    ),
                  })}
                </>
              ) : (
                <Alert variant="warning">Du må tildele deg oppgaven for å endre den</Alert>
              ))}
            <HStack gap="space-4" justify="space-between">
              <Button variant="tertiary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillOppgaveResult)}>
                Avbryt
              </Button>

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button variant="primary" onClick={ferdigstillOppgave} loading={isPending(ferdigstillOppgaveResult)}>
                  Ferdigstill oppgave
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
