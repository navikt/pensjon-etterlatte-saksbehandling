import { IBrev } from '~shared/types/Brev'
import { BrevMottakerPanel } from '~components/person/brev/mottaker/BrevMottakerPanel'
import { Alert, BodyShort, Button, HStack, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettMottaker, tilbakestillMottakere } from '~shared/api/brev'
import { isPending } from '~shared/api/apiUtils'
import { ArrowCirclepathIcon, PlusIcon } from '@navikt/aksel-icons'
import { ClickEvent, trackClick } from '~utils/analytics'

export const BrevMottakerWrapper = ({ brev, kanRedigeres }: { brev: IBrev; kanRedigeres: boolean }) => {
  const [mottakere, setMottakere] = useState(brev.mottakere)

  const [opprettMottakerResult, apiOpprettMottaker] = useApiCall(opprettMottaker)
  const [tilbakestillMottakereResult, apiTilbakestillMottaker] = useApiCall(tilbakestillMottakere)

  const tilbakestillMottakereWrapper = () => {
    trackClick(ClickEvent.TILBAKESTILL_MOTTAKERE_BREV)
    apiTilbakestillMottaker({ brevId: brev.id, sakId: brev.sakId }, (mottakereres) => {
      setMottakere([...mottakereres])
    })
  }

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
    <VStack gap="space-4">
      {!mottakere.length && <Alert variant="error">Brevet har ingen mottaker!</Alert>}

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

      {kanRedigeres && mottakere.length < 2 && (
        <HStack justify="center">
          <Button
            variant="secondary"
            onClick={opprettNyMottaker}
            loading={isPending(opprettMottakerResult)}
            icon={<PlusIcon aria-hidden />}
          >
            Legg til mottaker
          </Button>
        </HStack>
      )}
      {kanRedigeres && (
        <HStack justify="center">
          <BodyShort>
            Her kan du oppdatere mottakere, her bruker persongrunnlaget i saken til å generere mottakere. Hvis endringer
            har forekommet i familieforholdet i behandlingen vil dette bli gjenspeilet hvis denne brukes.
          </BodyShort>
          <Button
            variant="secondary"
            onClick={tilbakestillMottakereWrapper}
            loading={isPending(tilbakestillMottakereResult)}
            icon={<ArrowCirclepathIcon aria-hidden />}
          >
            Tilbakestill mottakere
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
