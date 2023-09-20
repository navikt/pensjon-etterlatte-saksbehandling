import { isPending, mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { opprettNyKlage } from '~shared/api/klage'
import { BodyShort, Button } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { useNavigate } from 'react-router-dom'
import { lenkeTilKlageMedId } from '~components/person/KlageListe'

export const FEATURE_TOGGLE_KAN_BRUKE_KLAGE = 'pensjon-etterlatte.kan-bruke-klage'

export function OpprettKlage(props: { sakId: number }) {
  const [opprettKlageStatus, opprettKlageKall] = useApiCall(opprettNyKlage)
  const navigate = useNavigate()

  function opprettKlage() {
    opprettKlageKall(props.sakId, (klage) => {
      navigate(lenkeTilKlageMedId(klage.id))
    })
  }

  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE)

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
