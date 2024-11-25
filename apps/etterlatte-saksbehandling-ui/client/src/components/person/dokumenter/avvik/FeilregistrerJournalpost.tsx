import { Alert, Button, HStack, Loader, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { feilregistrerSakstilknytning } from '~shared/api/dokument'
import { Journalpost } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ClickEvent, trackClick } from '~utils/amplitude'

export const FeilregistrerJournalpost = ({ journalpost }: { journalpost: Journalpost }) => {
  const [feilSakstilknytningStatus, apiFeilregistrerSakstilknytning] = useApiCall(feilregistrerSakstilknytning)

  const feilregistrer = () => {
    trackClick(ClickEvent.FEILREGISTRER_JOURNALPOST)

    apiFeilregistrerSakstilknytning(journalpost.journalpostId, () => {
      setTimeout(() => window.location.reload(), 2000)
    })
  }

  if (isSuccess(feilSakstilknytningStatus)) {
    return (
      <Alert variant="success">
        Journalposten ble feilregistrert. Laster siden på nytt <Loader />
      </Alert>
    )
  }

  return (
    <VStack gap="4">
      {journalpost.sak ? (
        <Alert variant="info">Du markerer nå journalposten som feilregistrert</Alert>
      ) : (
        <Alert variant="warning">
          Kan ikke feilregistrere sakstilknytning når journalposten ikke er tilknyttet en sak
        </Alert>
      )}

      {mapFailure(feilSakstilknytningStatus, (error) => (
        <ApiErrorAlert>{error.detail}</ApiErrorAlert>
      ))}

      <HStack justify="end">
        <Button
          variant="danger"
          onClick={feilregistrer}
          loading={isPending(feilSakstilknytningStatus)}
          disabled={!journalpost.sak || isSuccess(feilSakstilknytningStatus)}
        >
          Feilregistrer sakstilknytning
        </Button>
      </HStack>
    </VStack>
  )
}
