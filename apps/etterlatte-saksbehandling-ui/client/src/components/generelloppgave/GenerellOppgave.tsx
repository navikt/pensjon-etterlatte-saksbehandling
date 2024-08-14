import { Alert, Button, Heading, Textarea, TextField } from '@navikt/ds-react'
import React from 'react'

import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FormProvider, useForm } from 'react-hook-form'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { opprettGenerellOppgave } from '~shared/api/oppgaver'
import { GenerellOppgaveDto, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export default function GenerellOppgave() {
  useSidetittel('Opprett generell oppgave')
  const [generellOppgaveResult, opprettNyGenerellOppgave] = useApiCall(opprettGenerellOppgave)
  const methods = useForm<GenerellOppgaveDto>({
    defaultValues: {
      merknad: '',
      sakIds: '',
      type: Oppgavetype.GENERELL_OPPGAVE,
      kilde: OppgaveKilde.BEHANDLING,
    },
  })

  const opprett = (data: GenerellOppgaveDto) => {
    opprettNyGenerellOppgave({
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
    <FormWrapper>
      <FormProvider {...methods}>
        <Heading size="large">Opprett generell oppgave</Heading>

        <TextField
          {...register('sakIds', {
            required: { value: true, message: 'Du må spesifisere minimum EN saksID' },
          })}
          label="Saker"
          description="Legg til saksId i en kommaseparert liste"
          placeholder="sakid1, sakid2, ...."
          error={errors.sakIds?.message}
        />

        <Textarea
          {...register('merknad', {
            required: { value: true, message: 'Du må spesifisere merknad' },
          })}
          label="Merknad"
          placeholder="Legg til en kort beskrivelse av hva som skal utføres"
          error={errors.merknad?.message}
          maxLength={180}
        />

        <Button type="submit" onClick={handleSubmit(opprett)} variant="primary">
          Opprett
        </Button>
      </FormProvider>

      {mapAllApiResult(
        generellOppgaveResult,
        <Alert variant="info">Oppretter Generelle oppgaver.</Alert>,
        null,
        (error) => (
          <ApiErrorAlert>{error.detail}</ApiErrorAlert>
        ),
        () => (
          <Alert variant="success">Generelle oppgaver er opprettet!</Alert>
        )
      )}
    </FormWrapper>
  )
}
const FormWrapper = styled.div`
  margin: 2em;
  width: 25em;
  display: grid;
  gap: var(--a-spacing-4);
`
