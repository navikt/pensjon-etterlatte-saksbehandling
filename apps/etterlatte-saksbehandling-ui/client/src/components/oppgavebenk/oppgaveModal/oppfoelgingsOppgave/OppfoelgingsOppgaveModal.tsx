import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Heading, Label, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, hentOppgaveKommentarer, opprettOppgaveKommentar } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useForm } from 'react-hook-form'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { OppgaveKommentarer } from '~components/oppgavebenk/oppgaveModal/oppfoelgingsOppgave/OppgaveKommentarer'

interface OppfoegingsOppgaveSkjema {
  hvaSomErFulgtOpp: string
}

export function OppfoelgingAvOppgaveModal({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) {
  const [visModal, setVisModal] = useState(false)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erSaksbehandlersOppgave = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident

  const [hentOppgaveKommentarerResult, hentOppgaveKommentarerRequest] = useApiCall(hentOppgaveKommentarer)
  const [
    ferdigstillOppgaveMedMerknadResult,
    ferdigstillOppgaveMedMerknadRequest,
    resetFerdigstillOppgaveMedMerknadRequest,
  ] = useApiCall(ferdigstillOppgaveMedMerknad)

  const [opprettOppgaveKommentarResult, opprettOppgaveKommentarRequest, resetOpprettOppgaveKommentarRequest] =
    useApiCall(opprettOppgaveKommentar)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<OppfoegingsOppgaveSkjema>()

  const erRedigerbar = erOppgaveRedigerbar(oppgave.status) && erSaksbehandlersOppgave

  const lukkModal = () => {
    reset()
    resetOpprettOppgaveKommentarRequest()
    resetFerdigstillOppgaveMedMerknadRequest()
    setVisModal(false)
  }

  const lagreOppfoelging = (data: OppfoegingsOppgaveSkjema) => {
    opprettOppgaveKommentarRequest({ oppgaveId: oppgave.id, kommentar: data.hvaSomErFulgtOpp }, () => {
      hentOppgaveKommentarerRequest({ oppgaveId: oppgave.id }, () => {
        reset()
      })
    })
  }

  const ferdigstill = (data: OppfoegingsOppgaveSkjema) => {
    const merknad = oppgave.merknad
    ferdigstillOppgaveMedMerknadRequest({ id: oppgave.id, merknad }, (oppgave) => {
      opprettOppgaveKommentarRequest({ oppgaveId: oppgave.id, kommentar: data.hvaSomErFulgtOpp }, () => {
        oppdaterStatus(oppgave.id, oppgave.status)

        lukkModal()
      })
    })
  }

  useEffect(() => {
    hentOppgaveKommentarerRequest({ oppgaveId: oppgave.id })
  }, [])

  return (
    <>
      {erSaksbehandlersOppgave && (
        <Button size="small" onClick={() => setVisModal(true)}>
          Se oppgave
        </Button>
      )}
      {visModal && (
        <Modal open={visModal} onClose={lukkModal} header={{ heading: 'Oppfølging av sak' }}>
          <Box minWidth="42.5rem">
            <form onSubmit={handleSubmit(ferdigstill)}>
              <Modal.Body>
                <VStack gap="space-4">
                  {erRedigerbar && (
                    <BodyShort>
                      Når oppfølgingen er gjennomført, kan oppgaven ferdigstilles med en beskrivelse.
                    </BodyShort>
                  )}

                  {isFailureHandler({
                    apiResult: ferdigstillOppgaveMedMerknadResult,
                    errorMessage: 'Kunne ikke ferdigstille oppgaven.',
                  })}

                  {isFailureHandler({
                    apiResult: hentOppgaveKommentarerResult,
                    errorMessage: 'Kunne ikke hente historikk for oppgaven',
                  })}

                  {isFailureHandler({
                    apiResult: opprettOppgaveKommentarResult,
                    errorMessage: 'Kunne ikke opprette hva som ble fulgt opp',
                  })}
                  <VStack gap="space-4">
                    <VStack gap="space-2">
                      <Heading size="xsmall">{erRedigerbar ? 'Hva skal følges opp?' : 'Hva ble fulgt opp'}</Heading>
                      <BodyShort>{oppgave.merknad}</BodyShort>
                    </VStack>
                    <PersonLink fnr={oppgave.fnr!!} target="_blank" rel="noreferrer noopener">
                      Gå til saksoversikten <ExternalLinkIcon title="a11y-title" fontSize="1.3rem" />
                    </PersonLink>
                    <VStack gap="space-2">
                      <Label>Historikk</Label>
                      <OppgaveKommentarer hentOppgaveKommentarerResult={hentOppgaveKommentarerResult} />
                    </VStack>
                  </VStack>

                  {erRedigerbar &&
                    (erSaksbehandlersOppgave ? (
                      <>
                        <Textarea
                          {...register('hvaSomErFulgtOpp', {
                            required: {
                              value: true,
                              message: 'Du må skrive hva som ble fulgt opp for å avslutte oppgaven',
                            },
                          })}
                          label="Beskriv hva som er fulgt opp"
                          error={errors.hvaSomErFulgtOpp?.message}
                        />
                      </>
                    ) : (
                      <Alert variant="info">For å ferdigstille oppgaven må den være tildelt deg.</Alert>
                    ))}
                </VStack>
              </Modal.Body>
              <Modal.Footer>
                {erRedigerbar && (
                  <>
                    <Button
                      variant="primary"
                      type="submit"
                      loading={isPending(ferdigstillOppgaveMedMerknadResult)}
                      disabled={isPending(hentOppgaveKommentarerResult)}
                    >
                      Ferdigstill
                    </Button>
                    <Button
                      variant="secondary"
                      type="button"
                      loading={isPending(hentOppgaveKommentarerResult) || isPending(opprettOppgaveKommentarResult)}
                      onClick={handleSubmit(lagreOppfoelging)}
                    >
                      Lagre oppfølging
                    </Button>
                  </>
                )}
                <Button
                  onClick={lukkModal}
                  type="button"
                  variant="tertiary"
                  disabled={isPending(ferdigstillOppgaveMedMerknadResult)}
                >
                  Avbryt
                </Button>
              </Modal.Footer>
            </form>
          </Box>
        </Modal>
      )}
    </>
  )
}
