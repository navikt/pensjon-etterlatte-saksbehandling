import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { Button, Heading, HStack, Tag } from '@navikt/ds-react'
import { formaterSakstype } from '~utils/formatering/formatering'
import { settNyKlageRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useState } from 'react'
import { opprettKlageErUtfylt } from '~components/person/journalfoeringsoppgave/oppretteklage/OppsummeringKlagebehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'

export default function OpprettKlagebehandling() {
  const { oppgave, nyKlageRequest } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [feilmelding, setFeilmelding] = useState<string>('')

  if (!oppgave) {
    return <Navigate to="../" relative="path" />
  }

  const { sakType } = oppgave

  const neste = () => {
    if (!opprettKlageErUtfylt(nyKlageRequest)) {
      setFeilmelding('Klagedato må angis for å opprette klage fra oppgaven.')
      return
    }
    navigate('oppsummering', { relative: 'path' })
  }
  const tilbake = () => navigate('../', { relative: 'path' })

  return (
    <FormWrapper $column>
      <Heading size="medium" spacing>
        Opprett klage fra oppgave{' '}
        <Tag data-color="success" variant="outline" size="medium">
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>
      <DatoVelger
        value={nyKlageRequest?.mottattDato ? new Date(nyKlageRequest.mottattDato) : undefined}
        onChange={(mottattDato) =>
          dispatch(
            settNyKlageRequest({
              ...nyKlageRequest,
              mottattDato: mottattDato?.toISOString(),
            })
          )
        }
        label="Klagedato"
        description="Datoen klagen er framsatt av klager"
      />
      {feilmelding.length > 0 && <ApiErrorAlert>{feilmelding}</ApiErrorAlert>}
      <HStack gap="space-4" justify="center">
        <Button variant="secondary" onClick={tilbake}>
          Tilbake
        </Button>
        <Button variant="primary" onClick={neste}>
          Neste
        </Button>
      </HStack>
    </FormWrapper>
  )
}
