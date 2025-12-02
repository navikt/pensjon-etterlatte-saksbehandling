import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'
import { Alert, Button, HStack, Loader } from '@navikt/ds-react'
import { GosysActionToggle } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import Spinner from '~shared/Spinner'
import { ferdigstilleGosysOppgave } from '~shared/api/gosys'
import { GosysOppgave } from '~shared/types/Gosys'
import { ClickEvent, trackClick } from '~utils/analytics'

export const FerdigstillGosysOppgave = ({
  oppgave,
  setToggle,
}: {
  oppgave: GosysOppgave
  setToggle: (toggle: GosysActionToggle) => void
}) => {
  const [ferdigstillResult, ferdigstillOppgave] = useApiCall(ferdigstilleGosysOppgave)

  const ferdigstill = () => {
    trackClick(ClickEvent.FERDIGSTILL_GOSYS_OPPGAVE)

    ferdigstillOppgave({ oppgaveId: oppgave.id, versjon: oppgave.versjon || 0, enhetsnr: oppgave.enhet }, () => {
      setTimeout(() => window.location.reload(), 2000)
    })
  }

  return mapResult(ferdigstillResult, {
    success: () => (
      <Alert variant="success">
        Oppgaven ble ferdigstilt. Henter oppgaver på nytt <Loader />
      </Alert>
    ),
    error: (error) => (
      <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved ferdigstilling av oppgave'}</Alert>
    ),
    pending: <Spinner label="Ferdigstiller oppgaven..." />,
    initial: (
      <>
        <Alert variant="info">Er du sikker på at du vil ferdigstille oppgaven?</Alert>

        <br />

        <HStack gap="4" justify="end">
          <Button variant="secondary" onClick={() => setToggle({ ferdigstill: false })}>
            Nei, avbryt
          </Button>
          <Button onClick={ferdigstill}>Ja, ferdigstill</Button>
        </HStack>
      </>
    ),
  })
}
