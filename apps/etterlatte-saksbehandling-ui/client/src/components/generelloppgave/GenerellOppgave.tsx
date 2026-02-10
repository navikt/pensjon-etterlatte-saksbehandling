import { Alert, Button, Heading, Textarea, TextField, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useForm } from 'react-hook-form'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { opprettGenerellOppgave } from '~shared/api/oppgaver'
import { GenerellOppgaveDto, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { ClickEvent, trackClick } from '~utils/analytics'

interface GenerellOppgaveForm extends Omit<GenerellOppgaveDto, 'sakIds'> {
  sakIds: string
}

export default function GenerellOppgave() {
  useSidetittel('Opprett generell oppgave')
  const [opprettGenerellOppgaveResult, opprettGenerelOppgaveRequest] = useApiCall(opprettGenerellOppgave)
  const skalViseSide = useFeaturetoggle(FeatureToggle.opprette_generell_oppgave)

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<GenerellOppgaveForm>({
    defaultValues: {
      merknad: '',
      sakIds: '',
      type: Oppgavetype.GENERELL_OPPGAVE,
      kilde: OppgaveKilde.SAKSBEHANDLER,
    },
  })

  useEffect(() => {
    if (opprettGenerellOppgaveResult.status === 'success') {
      reset()
    }
  }, [opprettGenerellOppgaveResult, reset])

  const opprett = (data: GenerellOppgaveForm) => {
    trackClick(ClickEvent.OPPRETT_GENERELL_OPPGAVE)

    opprettGenerelOppgaveRequest({
      sakIds: data.sakIds
        .split(',')
        .filter((id) => id !== '')
        .map((id) => Number(id)),
      merknad: data.merknad,
      type: data.type,
      kilde: data.kilde,
    })
  }

  return (
    <VStack padding="space-8" gap="space-8" width="40rem">
      {skalViseSide && (
        <form onSubmit={handleSubmit(opprett)}>
          <VStack gap="space-4">
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

            {mapResult(opprettGenerellOppgaveResult, {
              pending: <Spinner label="Oppretter generelle oppgaver" />,
              error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
              success: () => <Alert variant="success">Generelle oppgaver er opprettet!</Alert>,
            })}
          </VStack>
        </form>
      )}

      {!skalViseSide && <Alert variant="error">Denne funksjonen er ikke tilgjengelig for denne brukerrollen.</Alert>}
    </VStack>
  )
}
