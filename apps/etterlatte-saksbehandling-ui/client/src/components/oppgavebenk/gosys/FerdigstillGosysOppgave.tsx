import { ferdigstilleGosysOppgave, OppgaveDTO } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { Alert, Button, Loader } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { GosysActionToggle } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'

export const FerdigstillGosysOppgave = ({
  oppgave,
  setToggle,
}: {
  oppgave: OppgaveDTO
  setToggle: (toggle: GosysActionToggle) => void
}) => {
  const [ferdigstillResult, ferdigstillOppgave] = useApiCall(ferdigstilleGosysOppgave)

  const ferdigstill = () =>
    ferdigstillOppgave({ oppgaveId: oppgave.id, versjon: oppgave.versjon || 0 }, () => {
      setTimeout(() => window.location.reload(), 2000)
    })

  if (isSuccess(ferdigstillResult)) {
    return (
      <Alert variant="success">
        Oppgaven ble ferdigstilt. Henter oppgaver på nytt <Loader />
      </Alert>
    )
  } else if (isFailure(ferdigstillResult)) {
    return (
      <Alert variant="error">
        {ferdigstillResult.error.detail || 'Ukjent feil oppsto ved ferdigstilling av oppgave'}
      </Alert>
    )
  }

  return (
    <>
      <Alert variant="info">Er du sikker på at du vil ferdigstille oppgaven?</Alert>

      <br />

      <FlexRow justify="right">
        <Button variant="secondary" onClick={() => setToggle({ ferdigstill: false })}>
          Nei, avbryt
        </Button>
        <Button onClick={ferdigstill} loading={isPending(ferdigstillResult)}>
          Ja, ferdigstill
        </Button>
      </FlexRow>
    </>
  )
}
