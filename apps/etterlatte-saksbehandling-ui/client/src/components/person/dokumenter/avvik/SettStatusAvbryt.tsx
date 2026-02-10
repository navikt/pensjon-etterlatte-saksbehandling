import { Alert, Button, HStack, Loader, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { settStatusAvbryt } from '~shared/api/dokument'
import { Journalpost, Journalposttype, Journalstatus } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

export const SettStatusAvbryt = ({ journalpost }: { journalpost: Journalpost }) => {
  const [settStatusAvbrytResult, apiSettStatusAvbryt] = useApiCall(settStatusAvbryt)

  const avbrytJournalpost = () =>
    apiSettStatusAvbryt(journalpost.journalpostId, () => {
      setTimeout(() => window.location.reload(), 2000)
    })

  if (journalpost.journalposttype === Journalposttype.I) {
    return <Alert variant="warning">Du kan ikke avbryte en inngående journalpost!</Alert>
  }

  if (journalpost.journalstatus === Journalstatus.FERDIGSTILT) {
    return <Alert variant="warning">Du kan ikke avbryte en ferdigstilt journalpost</Alert>
  }

  if (isSuccess(settStatusAvbrytResult)) {
    return (
      <Alert variant="success">
        Journalposten ble satt til status <strong>AVBRUTT</strong>. Laster siden på nytt... <Loader />
      </Alert>
    )
  }

  return (
    <VStack gap="space-4">
      <Alert variant="info">
        Du markerer nå journalposten med status <strong>AVBRUTT</strong>
      </Alert>

      {mapFailure(settStatusAvbrytResult, (error) => (
        <ApiErrorAlert>{error.detail}</ApiErrorAlert>
      ))}

      <HStack justify="end">
        <Button variant="danger" onClick={avbrytJournalpost} loading={isPending(settStatusAvbrytResult)}>
          Avbryt journalpost
        </Button>
      </HStack>
    </VStack>
  )
}
