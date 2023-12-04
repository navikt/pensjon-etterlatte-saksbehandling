import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { useEffect } from 'react'
import { Status } from '~shared/types/IFeature'
import { mapSuccess } from '~shared/api/apiUtils'

export const useFeatureEnabledMedDefault = (toggle: string, defaultValue: boolean): boolean => {
  const [feature, fetchFeature] = useApiCall(hentFunksjonsbrytere)

  useEffect(() => {
    fetchFeature([toggle])
  }, [])

  return (
    mapSuccess(feature, ([featureToggle]) => {
      switch (featureToggle.enabled) {
        case Status.HENTING_FEILA:
          return defaultValue
        case Status.UDEFINERT:
          return defaultValue
        case Status.AV:
          return false
        case Status.PAA:
          return true
      }
    }) ?? defaultValue
  )
}
