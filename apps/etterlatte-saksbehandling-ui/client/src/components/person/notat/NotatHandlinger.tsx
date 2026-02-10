import { Alert, Button, HStack, VStack } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { journalfoerNotat } from '~shared/api/notat'
import { NotatRedigeringFane } from '~components/person/notat/NotatRedigeringModal'
import { formaterTidspunktTimeMinutterSekunder } from '~utils/formatering/dato'
import { ClickEvent, trackClick } from '~utils/analytics'

export const NotatHandlinger = ({
  notatId,
  setFane,
  lagre,
  sistLagret,
  lukkModal,
}: {
  notatId: number
  setFane: (fane: NotatRedigeringFane) => void
  lagre: () => void
  sistLagret: Date | undefined
  lukkModal: () => void
}) => {
  const [toggleJournalfoer, setToggleJournalfoer] = useState(false)

  const [journalfoerStatus, apiJournalfoerNotat] = useApiCall(journalfoerNotat)

  const journalfoer = () => {
    trackClick(ClickEvent.JOURNALFOER_NOTAT)

    apiJournalfoerNotat(notatId, () => {
      window.location.reload()
    })
  }

  return toggleJournalfoer ? (
    <VStack gap="space-4">
      <Alert variant="warning">Er du sikker på at du vil journalføre notatet? Handlingen kan ikke angres.</Alert>

      <HStack gap="space-4" align="center" justify="end">
        <Button variant="tertiary" onClick={() => setToggleJournalfoer(false)}>
          Nei, avbryt
        </Button>
        <Button onClick={journalfoer} loading={isPending(journalfoerStatus)}>
          Ja, journalfør
        </Button>
      </HStack>
    </VStack>
  ) : (
    <HStack justify="space-between">
      <Button variant="tertiary" onClick={lukkModal}>
        Lukk
      </Button>

      <HStack gap="space-4" align="center">
        {!!sistLagret && (
          <Alert variant="success" size="small" inline>
            Sist lagret kl. {formaterTidspunktTimeMinutterSekunder(sistLagret)}
          </Alert>
        )}

        <Button variant="secondary" onClick={lagre}>
          Lagre
        </Button>
        <Button
          onClick={() => {
            setFane(NotatRedigeringFane.FORHAANDSVIS)
            setToggleJournalfoer(true)
          }}
        >
          Journalfør
        </Button>
      </HStack>
    </HStack>
  )
}
