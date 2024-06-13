import { Alert, Button, HStack, VStack } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { journalfoerNotat } from '~shared/api/notat'
import { NotatRedigeringFane } from '~components/person/notat/NotatRedigeringModal'
import { formaterTidspunktTimeMinutterSekunder } from '~utils/formattering'

export const JournalfoerNotat = ({
  notatId,
  setFane,
  sistLagret,
  lukkModal,
}: {
  notatId: number
  setFane: (fane: NotatRedigeringFane) => void
  sistLagret: Date | undefined
  lukkModal: () => void
}) => {
  const [toggleJournalfoer, setToggleJournalfoer] = useState(false)

  const [journalfoerStatus, apiJournalfoerNotat] = useApiCall(journalfoerNotat)

  const journalfoer = () => {
    apiJournalfoerNotat(notatId, () => {
      window.location.reload()
    })
  }

  return toggleJournalfoer ? (
    <VStack gap="4">
      <Alert variant="warning">Er du sikker på at du vil journalføre notatet? Handlingen kan ikke angres.</Alert>

      <HStack gap="4" align="center">
        <Button variant="tertiary" onClick={() => setToggleJournalfoer(false)}>
          Nei, avbryt
        </Button>
        <Button onClick={journalfoer} loading={isPending(journalfoerStatus)}>
          Ja, journalfør
        </Button>
      </HStack>
    </VStack>
  ) : (
    <HStack gap="4" align="center">
      {!!sistLagret && (
        <Alert variant="success" size="small" inline>
          Sist lagret kl. {formaterTidspunktTimeMinutterSekunder(sistLagret)}
        </Alert>
      )}

      <Button variant="tertiary" onClick={lukkModal}>
        Lukk
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
  )
}
