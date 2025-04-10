import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Heading, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useForm } from 'react-hook-form'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { PersonLink } from '~components/person/lenker/PersonLink'

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
  const [tildelSaksbehandlerResult, tildelSaksbehandlerRequest] = useApiCall(tildelSaksbehandlerApi)

  const [ferdigstillOppgaveMedMerknadResult, ferdigstillOppgaveMedMerknadRequest] =
    useApiCall(ferdigstillOppgaveMedMerknad)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OppfoegingsOppgaveSkjema>()

  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  const ferdigstill = (data: OppfoegingsOppgaveSkjema) => {
    if (erRedigerbar) {
      const merknad = data.hvaSomErFulgtOpp ? data.hvaSomErFulgtOpp : oppgave.merknad
      ferdigstillOppgaveMedMerknadRequest({ id: oppgave.id, merknad: merknad }, (oppgave) => {
        setVisModal(false)
        oppdaterStatus(oppgave.id, oppgave.status)
      })
    }
  }
  const tildelOppgave = () => {
    tildelSaksbehandlerRequest({ oppgaveId: oppgave.id, saksbehandlerIdent: innloggetSaksbehandler.ident })
  }

  return (
    <>
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal open={visModal} onClose={() => setVisModal(false)} header={{ heading: 'Oppfølging av sak' }}>
          <Box minWidth="41.5rem">
            <form onSubmit={handleSubmit(ferdigstill)}>
              <Modal.Body>
                <VStack gap="4">
                  {erRedigerbar && (
                    <BodyShort>Når oppfølgingen er gjort kan oppgaven ferdigstilles med en beskrivelse.</BodyShort>
                  )}

                  {isFailureHandler({
                    apiResult: ferdigstillOppgaveMedMerknadResult,
                    errorMessage: 'Kunne ikke ferdigstille oppgaven.',
                  })}

                  {isFailureHandler({
                    apiResult: tildelSaksbehandlerResult,
                    errorMessage: 'Kunne ikke tildele oppgaven til deg.',
                  })}
                  <VStack gap="2">
                    <Heading size="xsmall">{erRedigerbar ? 'Hva skal følges opp?' : 'Hva ble fulgt opp'}</Heading>
                    <BodyShort>{oppgave.merknad}</BodyShort>
                    <PersonLink fnr={oppgave.fnr!!} target="_blank" rel="noreferrer noopener">
                      Gå til saksoversikten <ExternalLinkIcon title="a11y-title" fontSize="1.3rem" />
                    </PersonLink>
                  </VStack>

                  {erRedigerbar &&
                    (erSaksbehandlersOppgave ? (
                      <Textarea
                        {...register('hvaSomErFulgtOpp', {
                          required: {
                            value: true,
                            message: 'Du må skrive hva som ble fulgt opp for å avslutte oppgaven',
                          },
                        })}
                        error={errors.hvaSomErFulgtOpp?.message}
                        label="Beskriv hva som er fulgt opp"
                      />
                    ) : (
                      <Alert variant="info">For å ferdigstille oppgaven må den være tildelt deg.</Alert>
                    ))}
                </VStack>
              </Modal.Body>
              <Modal.Footer>
                {erRedigerbar &&
                  (erSaksbehandlersOppgave ? (
                    <Button variant="primary" type="submit" loading={isPending(ferdigstillOppgaveMedMerknadResult)}>
                      Ferdigstill
                    </Button>
                  ) : (
                    <Button variant="primary" onClick={tildelOppgave} loading={isPending(tildelSaksbehandlerResult)}>
                      Tildel meg
                    </Button>
                  ))}
                <Button
                  onClick={() => setVisModal(false)}
                  variant="tertiary"
                  disabled={isPending(ferdigstillOppgaveMedMerknadResult) || isPending(tildelSaksbehandlerResult)}
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
