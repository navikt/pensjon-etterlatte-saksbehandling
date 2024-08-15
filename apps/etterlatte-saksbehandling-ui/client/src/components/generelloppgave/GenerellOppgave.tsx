import { Alert, Button, Heading, Textarea, TextField, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FormProvider, useForm } from 'react-hook-form'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { opprettGenerellOppgave } from '~shared/api/oppgaver'
import { GenerellOppgaveDto, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

export default function GenerellOppgave() {
  useSidetittel('Opprett generell oppgave')
  const [opprettGenerellOppgaveResult, opprettGenerelOppgaveRequest] = useApiCall(opprettGenerellOppgave)
  const skalViseSide = useFeatureEnabledMedDefault('opprette-generell-oppgave', false)

  const methods = useForm<GenerellOppgaveDto>({
    defaultValues: {
      merknad: '',
      sakIds: '',
      type: Oppgavetype.GENERELL_OPPGAVE,
      kilde: OppgaveKilde.BEHANDLING,
    },
  })

  const { reset } = methods

  useEffect(() => {
    if (opprettGenerellOppgaveResult.status === 'success') {
      reset()
    }
  }, [opprettGenerellOppgaveResult, reset])

  const opprett = (data: GenerellOppgaveDto) => {
    opprettGenerelOppgaveRequest({
      sakIds: data.sakIds,
      merknad: data.merknad,
      type: data.type,
      kilde: data.kilde,
    })
  }

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = methods

  return (
    <VStack padding="8" gap="4" width="40rem">
      {skalViseSide && (
        <>
          <FormProvider {...methods}>
            <Heading size="large">Opprett generell oppgave</Heading>

            <TextField
              {...register('sakIds', {
                required: { value: true, message: 'Du må oppgi minst en saks-ID' },
              })}
              label="Saker"
              description="Legg til saks-ID-er i en kommaseparert liste"
              placeholder="sakid1, sakid2, ...."
              error={errors.sakIds?.message}
            />

            <Textarea
              {...register('merknad', {
                required: { value: true, message: 'Du må spesifisere merknad' },
              })}
              label="Merknad"
              placeholder="Legg til en kort beskrivelse av oppgaven"
              error={errors.merknad?.message}
              maxLength={180}
            />

            <Button type="submit" onClick={handleSubmit(opprett)} variant="primary">
              Opprett
            </Button>
          </FormProvider>

          {mapAllApiResult(
            opprettGenerellOppgaveResult,
            <Alert variant="info">Oppretter Generelle oppgaver.</Alert>,
            null,
            (error) => (
              <ApiErrorAlert>{error.detail}</ApiErrorAlert>
            ),
            () => (
              <Alert variant="success">Generelle oppgaver er opprettet!</Alert>
            )
          )}
        </>
      )}

      {!skalViseSide && <Alert variant="error">Denne funksjonen er ikke tilgjengelig for denne brukerrollen.</Alert>}
    </VStack>
  )
}
