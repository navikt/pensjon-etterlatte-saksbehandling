import { isPending, mapApiResult, mapSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { opprettNyKlage } from '~shared/api/klage'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import React, { useEffect } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'

const FEATURE_TOGGLE_KAN_BRUKE_KLAGE = 'pensjon-etterlatte.kan-bruke-klage'

export function OpprettKlage(props: { sakId: number }) {
  const [opprettKlageStatus, opprettKlageKall] = useApiCall(opprettNyKlage)
  const [brytere, getBrytere] = useApiCall(hentFunksjonsbrytere)

  useEffect(() => {
    getBrytere([FEATURE_TOGGLE_KAN_BRUKE_KLAGE])
  }, [])

  function opprettKlage() {
    opprettKlageKall(props.sakId)
  }

  const kanBrukeKlage = mapSuccess(brytere, ([toggleKanBrukeKlage]) => toggleKanBrukeKlage.enabled) ?? false

  if (!kanBrukeKlage) {
    return null
  }

  return mapApiResult(
    opprettKlageStatus,
    <Button onClick={opprettKlage} loading={isPending(opprettKlageStatus)}>
      Opprett klage
    </Button>,
    () => <ApiErrorAlert>Kunne ikke opprette klage</ApiErrorAlert>,
    (klage) => <BodyShort>Klage med id={klage.id} er opprettet</BodyShort>
  )
}
