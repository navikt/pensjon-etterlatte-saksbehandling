import { ISak } from '~shared/types/sak'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOppgave } from '~shared/api/oppgaver'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { useForm } from 'react-hook-form'
import { ClickEvent, trackClick } from '~utils/amplitude'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Checkbox, CheckboxGroup, Modal, Textarea, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterDato } from '~utils/formatering/dato'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

interface OppfoelgingsOpppgaveForm {
  merknad: string
  frist: Date
  tildelMeg: boolean
}

export function OpprettOppfoelgingsoppgaveModal(props: { sak: ISak; vedOpprettelse?: () => void }) {
  const { sak, vedOpprettelse } = props
  const [open, setOpen] = useState(false)
  const [opprettOppgaveStatus, opprettOppgaveApi, resetOpprettOppgaveStatus] = useApiCall(opprettOppgave)
  const saksbehandler = useInnloggetSaksbehandler()

  const kanOppretteOppfoelgingsoppgave = useFeaturetoggle(FeatureToggle.opprette_oppfoelgingsoppgave)

  const {
    register,
    control,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm<OppfoelgingsOpppgaveForm>()

  useEffect(() => {
    reset()
    resetOpprettOppgaveStatus()
  }, [open])

  function opprettOppfoelgingsoppgave(formdata: OppfoelgingsOpppgaveForm) {
    const justertFrist = new Date(formdata.frist)
    justertFrist.setHours(12)
    trackClick(ClickEvent.OPPRETT_OPPFOELGINGSOPPGAVE)
    opprettOppgaveApi(
      {
        sakId: sak.id,
        request: {
          oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
          oppgaveType: Oppgavetype.OPPFOELGING,
          merknad: formdata.merknad,
          frist: justertFrist.toISOString(),
          saksbehandler: formdata.tildelMeg ? saksbehandler.ident : undefined,
        },
      },
      vedOpprettelse
    )
  }

  if (!kanOppretteOppfoelgingsoppgave) {
    return null
  }

  return (
    <Box>
      <Button
        onClick={() => {
          trackClick(ClickEvent.AAPNE_OPPFOELGINGSOPPGAVE_MODAL)
          setOpen(true)
        }}
        variant="secondary"
      >
        Opprett oppfølgingsoppgave
      </Button>
      <Modal open={open} onClose={() => setOpen(false)} header={{ heading: 'Opprett oppfølgingsoppgave' }}>
        <Box width="40rem">
          <form onSubmit={handleSubmit(opprettOppfoelgingsoppgave)}>
            <Modal.Body>
              <VStack gap="4">
                {mapResult(opprettOppgaveStatus, {
                  initial: (
                    <BodyShort>
                      En oppfølgingsoppgave har en egendefinert frist og merknad, og kan brukes til å lage påminnelser
                      om noe som skal følges opp i saken.
                    </BodyShort>
                  ),
                  pending: <Spinner label="Oppretter oppfølgingsoppgaven" />,
                  success: (opprettetOppgave) => (
                    <Alert variant="success">
                      Oppfølgingsoppgaven er opprettet, med merknad {opprettetOppgave.merknad} og frist{' '}
                      {formaterDato(opprettetOppgave.frist)}.
                    </Alert>
                  ),
                })}
                {!isSuccess(opprettOppgaveStatus) && (
                  <>
                    <ControlledDatoVelger
                      name="frist"
                      label="Frist for oppgaven"
                      control={control}
                      errorVedTomInput="Du må angi frist for oppgaven"
                    />
                    <Textarea
                      {...register('merknad', {
                        required: {
                          value: true,
                          message: 'Du må si hva som skal følges opp i oppgaven',
                        },
                      })}
                      label="Merknad"
                      description="Beskriv kort hva som skal følges opp i oppgaven"
                      error={errors.merknad?.message}
                      disabled={isPending(opprettOppgaveStatus)}
                    />

                    <CheckboxGroup legend="Tildel meg" description="Skal oppfølgingsoppgaven være tildelt deg?">
                      <Checkbox {...register('tildelMeg')}>Tildel oppgaven til meg</Checkbox>
                    </CheckboxGroup>
                  </>
                )}
              </VStack>
            </Modal.Body>
            <Modal.Footer>
              {isSuccess(opprettOppgaveStatus) ? (
                <Button variant="primary" onClick={() => setOpen(false)}>
                  Lukk
                </Button>
              ) : (
                <>
                  <Button variant="primary" type="submit" loading={isPending(opprettOppgaveStatus)}>
                    Lag oppgave
                  </Button>
                  <Button variant="tertiary" onClick={() => setOpen(false)} disabled={isPending(opprettOppgaveStatus)}>
                    Avbryt
                  </Button>
                </>
              )}
            </Modal.Footer>
          </form>
        </Box>
      </Modal>
    </Box>
  )
}
