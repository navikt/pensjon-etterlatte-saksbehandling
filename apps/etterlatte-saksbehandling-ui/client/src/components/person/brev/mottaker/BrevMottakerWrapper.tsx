import { IBrev } from '~shared/types/Brev'
import { BrevMottakerPanel } from '~components/person/brev/mottaker/BrevMottakerPanel'
import { Button, HStack, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettMottaker } from '~shared/api/brev'
import { isPending } from '~shared/api/apiUtils'
import { PlusIcon } from '@navikt/aksel-icons'

export const BrevMottakerWrapper = ({ brev, kanRedigeres }: { brev: IBrev; kanRedigeres: boolean }) => {
  const [mottakere, setMottakere] = useState(brev.mottakere)

  const [opprettMottakerResult, apiOpprettMottaker] = useApiCall(opprettMottaker)

  const opprettNyMottaker = () => {
    apiOpprettMottaker({ brevId: brev.id, sakId: brev.sakId }, (mottaker) => {
      setMottakere([...mottakere, mottaker])
    })
  }

  const fjernMottaker = (mottakerId: string) => {
    const mottakereOppdatert = mottakere.filter((mottaker) => mottaker.id !== mottakerId)

    setMottakere(mottakereOppdatert)
  }

  return (
    <VStack gap="4">
      {mottakere.map((mottaker) => (
        <BrevMottakerPanel
          key={mottaker.id}
          brevId={brev.id}
          behandlingId={brev.behandlingId}
          sakId={brev.sakId}
          mottaker={mottaker}
          kanRedigeres={kanRedigeres}
          flereMottakere={mottakere.length > 1}
          fjernMottaker={fjernMottaker}
        />
      ))}

      {mottakere.length < 2 && (
        <HStack justify="center">
          <Button
            variant="secondary"
            onClick={opprettNyMottaker}
            loading={isPending(opprettMottakerResult)}
            icon={<PlusIcon />}
          >
            Legg til mottaker
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
