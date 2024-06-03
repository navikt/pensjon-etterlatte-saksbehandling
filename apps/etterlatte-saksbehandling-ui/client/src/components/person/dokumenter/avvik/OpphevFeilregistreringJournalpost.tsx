import { Alert, Box, Button, HStack, Loader, VStack } from '@navikt/ds-react'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opphevFeilregistrertSakstilknytning } from '~shared/api/dokument'
import { Journalpost } from '~shared/types/Journalpost'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

export const OpphevFeilregistreringJournalpost = ({ journalpost }: { journalpost: Journalpost }) => {
  const [opphevFeilregistreringStatus, apiOpphevFeilregistrering] = useApiCall(opphevFeilregistrertSakstilknytning)

  const opphevFeilregistrering = () =>
    apiOpphevFeilregistrering(journalpost.journalpostId, () => {
      setTimeout(() => window.location.reload(), 2000)
    })

  if (isSuccess(opphevFeilregistreringStatus)) {
    return (
      <Alert variant="success">
        Feilregistrering av journalpost ble opphevet. Laster siden på nytt <Loader />
      </Alert>
    )
  }

  return (
    <>
      <Alert variant="info">Du opphever nå status feilregistrert på journalposten</Alert>
      <br />

      <Box borderWidth="1" padding="4" borderRadius="medium" borderColor="border-subtle">
        <VStack gap="4">
          <Info label="Sakstype" tekst={journalpost.sak?.sakstype || '-'} />
          <Info label="FagsakId" tekst={journalpost.sak?.fagsakId || '-'} />
          <Info label="Fagsaksystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
          <Info label="Tema" tekst={journalpost.sak?.tema || '-'} />
        </VStack>
      </Box>

      <br />
      <HStack justify="end">
        <Button onClick={opphevFeilregistrering} loading={isPending(opphevFeilregistreringStatus)}>
          Opphev feilregistrering
        </Button>
      </HStack>
    </>
  )
}
