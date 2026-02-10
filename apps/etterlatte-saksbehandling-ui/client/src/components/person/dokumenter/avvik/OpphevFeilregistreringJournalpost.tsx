import { Alert, Box, Button, HStack, Loader, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opphevFeilregistrertSakstilknytning } from '~shared/api/dokument'
import { Journalpost } from '~shared/types/Journalpost'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ClickEvent, trackClick } from '~utils/analytics'

export const OpphevFeilregistreringJournalpost = ({ journalpost }: { journalpost: Journalpost }) => {
  const [opphevFeilregistreringStatus, apiOpphevFeilregistrering] = useApiCall(opphevFeilregistrertSakstilknytning)

  const opphevFeilregistrering = () => {
    trackClick(ClickEvent.OPPHEV_FEILREGISTRERING_JOURNALPOST)

    apiOpphevFeilregistrering(journalpost.journalpostId, () => {
      setTimeout(() => window.location.reload(), 2000)
    })
  }

  if (isSuccess(opphevFeilregistreringStatus)) {
    return (
      <Alert variant="success">
        Feilregistrering av journalpost ble opphevet. Laster siden på nytt <Loader />
      </Alert>
    )
  }

  return (
    <VStack gap="space-4">
      <Alert variant="info">Du opphever nå status feilregistrert på journalposten</Alert>

      <Box borderWidth="1" padding="space-4">
        <VStack gap="space-4">
          <Info label="Sakstype" tekst={journalpost.sak?.sakstype || '-'} />
          <Info label="FagsakId" tekst={journalpost.sak?.fagsakId || '-'} />
          <Info label="Fagsaksystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
          <Info label="Tema" tekst={journalpost.sak?.tema || '-'} />
        </VStack>
      </Box>

      {mapFailure(opphevFeilregistreringStatus, (error) => (
        <ApiErrorAlert>{error.detail}</ApiErrorAlert>
      ))}

      <HStack justify="end">
        <Button onClick={opphevFeilregistrering} loading={isPending(opphevFeilregistreringStatus)}>
          Opphev feilregistrering
        </Button>
      </HStack>
    </VStack>
  )
}
