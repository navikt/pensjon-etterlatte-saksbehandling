import { Alert, Button, Loader } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { feilregistrerSakstilknytning } from '~shared/api/dokument'
import { Journalpost } from '~shared/types/Journalpost'

export const FeilregistrerJournalpost = ({ journalpost }: { journalpost: Journalpost }) => {
  const [feilSakstilknytningStatus, apiFeilregistrerSakstilknytning] = useApiCall(feilregistrerSakstilknytning)

  const feilregistrer = () =>
    apiFeilregistrerSakstilknytning(journalpost.journalpostId, () => {
      setTimeout(() => window.location.reload(), 2000)
    })

  if (isSuccess(feilSakstilknytningStatus)) {
    return (
      <Alert variant="success">
        Journalposten ble feilregistrert. Laster siden på nytt <Loader />
      </Alert>
    )
  }

  return (
    <>
      {journalpost.sak ? (
        <Alert variant="info">Du markerer nå journalposten som feilregistrert</Alert>
      ) : (
        <Alert variant="warning">
          Kan ikke feilregistrere sakstilknytning når journalposten ikke er tilknyttet en sak
        </Alert>
      )}
      <br />

      <br />
      <FlexRow justify="right">
        <Button
          variant="danger"
          onClick={feilregistrer}
          loading={isPending(feilSakstilknytningStatus)}
          disabled={!journalpost.sak || isSuccess(feilSakstilknytningStatus)}
        >
          Feilregistrer sakstilknytning
        </Button>
      </FlexRow>
    </>
  )
}
