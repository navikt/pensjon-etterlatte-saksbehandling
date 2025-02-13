import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Heading, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, hentOppgave, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useForm } from 'react-hook-form'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { PersonLink } from '~components/person/lenker/PersonLink'

interface FormData {
  merknad: string
}

export function OppfoelgingAvOppgaveModal(props: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) {
  const [visModal, setVisModal] = useState(false)
  const { oppgave: oppgaveProp, oppdaterStatus } = props
  const [oppgave, setOppgave] = useState<OppgaveDTO>(oppgaveProp)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erSaksbehandlersOppgave = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const [tildelStatus, tildelApi] = useApiCall(tildelSaksbehandlerApi)

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [hentOppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormData>()

  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  useEffect(() => {
    if (visModal) {
      apiHentOppgave(oppgave.id, (result) => {
        setOppgave(result)
      })
    }
  }, [visModal])
  const merknadSkjema = watch('merknad')

  const ferdigstill = () => {
    if (erRedigerbar) {
      const merknad = merknadSkjema ? merknadSkjema : oppgave.merknad
      apiFerdigstillOppgave({ id: oppgave.id, merknad: merknad }, (oppgave) => {
        setVisModal(false)
        oppdaterStatus(oppgave.id, oppgave.status)
      })
    }
  }
  const tildelOppgave = () => {
    tildelApi({ oppgaveId: oppgave.id, saksbehandlerIdent: innloggetSaksbehandler.ident }, () => {
      apiHentOppgave(oppgave.id, (result) => setOppgave(result))
    })
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
                    apiResult: ferdigstillOppgaveStatus,
                    errorMessage: 'Kunne ikke ferdigstille oppgaven.',
                  })}

                  {isFailureHandler({
                    apiResult: hentOppgaveStatus,
                    errorMessage: 'Kunne ikke hente ut oppgaven. Prøv å last Gjenny på nytt.',
                  })}

                  {isFailureHandler({
                    apiResult: tildelStatus,
                    errorMessage: 'Kunne ikke tildele oppgaven til deg.',
                  })}
                  <div>
                    <Heading size="xsmall">{erRedigerbar ? 'Hva skal følges opp?' : 'Hva ble fulgt opp'}</Heading>
                    <BodyShort>
                      {oppgave.merknad}.{' '}
                      <PersonLink fnr={oppgave.fnr!!} target="_blank" rel="noreferrer noopener">
                        Gå til saksoversikten <ExternalLinkIcon title="a11y-title" fontSize="1.3rem" />
                      </PersonLink>
                    </BodyShort>
                  </div>

                  {erRedigerbar &&
                    (erSaksbehandlersOppgave ? (
                      <Textarea
                        {...register('merknad', {
                          required: {
                            value: true,
                            message: 'Du må skrive hva som ble fulgt opp for å avslutte oppgaven',
                          },
                        })}
                        error={errors.merknad?.message}
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
                    <Button variant="primary" type="submit" loading={isPending(ferdigstillOppgaveStatus)}>
                      Ferdigstill
                    </Button>
                  ) : (
                    <Button
                      variant="primary"
                      onClick={tildelOppgave}
                      loading={isPending(hentOppgaveStatus) || isPending(tildelStatus)}
                    >
                      Tildel meg
                    </Button>
                  ))}
                <Button
                  onClick={() => setVisModal(false)}
                  variant="tertiary"
                  disabled={
                    isPending(ferdigstillOppgaveStatus) || isPending(hentOppgaveStatus) || isPending(tildelStatus)
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
