import { erOppgaveRedigerbar, OppgaveDTO, OppgaveKilde, Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Checkbox, Heading, Label, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, opprettOppgave, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useForm } from 'react-hook-form'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { ClickEvent, trackClick } from '~utils/amplitude'
import { OppgaveKommentarer } from '~components/oppgavebenk/oppgaveModal/oppfoelgingsOppgave/OppgaveKommentarer'

interface OppfoegingsOppgaveSkjema {
  hvaSomErFulgtOpp: string
  skalOppretteNyOppfoelgingsOppgave: boolean
  nyOppfoelgingsOppgaveFrist: string
  nyOppfoelgingsOppgaveMerknad: string
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
  const [tildelSaksbehandlerResult, tildelSaksbehandlerRequest, resetTildelSaksbehandlerRequest] =
    useApiCall(tildelSaksbehandlerApi)

  const [opprettOppgaveResult, opprettOppgaveRequest, resetOpprettOppgaveRequest] = useApiCall(opprettOppgave)
  const [
    ferdigstillOppgaveMedMerknadResult,
    ferdigstillOppgaveMedMerknadRequest,
    resetFerdigstillOppgaveMedMerknadRequest,
  ] = useApiCall(ferdigstillOppgaveMedMerknad)

  const {
    register,
    handleSubmit,
    watch,
    control,
    reset,
    formState: { errors },
  } = useForm<OppfoegingsOppgaveSkjema>({
    defaultValues: {
      nyOppfoelgingsOppgaveMerknad: oppgave.merknad,
      nyOppfoelgingsOppgaveFrist: oppgave.frist,
    },
  })

  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  const lukkModal = () => {
    reset()
    resetOpprettOppgaveRequest()
    resetFerdigstillOppgaveMedMerknadRequest()
    resetTildelSaksbehandlerRequest()
    setVisModal(false)
  }

  const ferdigstill = (data: OppfoegingsOppgaveSkjema) => {
    const merknad = data.hvaSomErFulgtOpp ? data.hvaSomErFulgtOpp : oppgave.merknad
    ferdigstillOppgaveMedMerknadRequest({ id: oppgave.id, merknad: merknad }, (oppgave) => {
      if (data.skalOppretteNyOppfoelgingsOppgave) {
        const justertFrist = new Date(data.nyOppfoelgingsOppgaveFrist)
        justertFrist.setHours(12)

        opprettOppgaveRequest(
          {
            sakId: oppgave.sakId,
            request: {
              oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
              oppgaveType: Oppgavetype.OPPFOELGING,
              merknad: data.nyOppfoelgingsOppgaveMerknad,
              frist: justertFrist.toISOString(),
              saksbehandler: innloggetSaksbehandler.ident,
            },
          },
          () => {
            trackClick(ClickEvent.OPPRETT_OPPFOELGINGSOPPGAVE)
            lukkModal()
            oppdaterStatus(oppgave.id, oppgave.status)
          }
        )
      } else {
        lukkModal()
        oppdaterStatus(oppgave.id, oppgave.status)
      }
    })
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
        <Modal open={visModal} onClose={lukkModal} header={{ heading: 'Oppfølging av sak' }}>
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
                    apiResult: opprettOppgaveResult,
                    errorMessage: 'Kunne ikke opprette ny oppgave',
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
                    <Label>Historikk</Label>
                    <OppgaveKommentarer />
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
                        <Checkbox {...register('skalOppretteNyOppfoelgingsOppgave')}>
                          Lag ny oppfølgingsoppgave
                        </Checkbox>
                        {!!watch('skalOppretteNyOppfoelgingsOppgave') && (
                          <>
                            <ControlledDatoVelger
                              name="nyOppfoelgingsOppgaveFrist"
                              label="Frist for ny oppgave"
                              control={control}
                              errorVedTomInput="Du må sette en frist"
                            />
                            <Textarea
                              {...register('nyOppfoelgingsOppgaveMerknad', {
                                required: {
                                  value: true,
                                  message: 'Du må gi en merknad for den nye oppgaven',
                                },
                              })}
                              label="Merknad på ny oppgave"
                              error={errors.nyOppfoelgingsOppgaveMerknad?.message}
                            />
                          </>
                        )}
                      </>
                    ) : (
                      <Alert variant="info">For å ferdigstille oppgaven må den være tildelt deg.</Alert>
                    ))}
                </VStack>
              </Modal.Body>
              <Modal.Footer>
                {erRedigerbar &&
                  (erSaksbehandlersOppgave ? (
                    <Button
                      variant="primary"
                      type="submit"
                      loading={isPending(ferdigstillOppgaveMedMerknadResult) || isPending(opprettOppgaveResult)}
                    >
                      {!!watch('skalOppretteNyOppfoelgingsOppgave') ? 'Ferdigstill og opprett ny' : 'Ferdigstill'}
                    </Button>
                  ) : (
                    <Button
                      variant="primary"
                      type="button"
                      onClick={tildelOppgave}
                      loading={isPending(tildelSaksbehandlerResult)}
                    >
                      Tildel meg
                    </Button>
                  ))}
                <Button
                  onClick={lukkModal}
                  type="button"
                  variant="tertiary"
                  disabled={
                    isPending(ferdigstillOppgaveMedMerknadResult) ||
                    isPending(tildelSaksbehandlerResult) ||
                    isPending(opprettOppgaveResult)
                  }
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
