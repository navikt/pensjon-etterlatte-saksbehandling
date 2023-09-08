import { mapSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { useEffect } from 'react'

export const useFeatureEnabledMedDefault = (toggle: string, defaultValue: boolean = false): boolean => {
  const [feature, fetchFeature] = useApiCall(hentFunksjonsbrytere)

  useEffect(() => {
    fetchFeature([toggle])
  }, [])

  return mapSuccess(feature, ([featureToggle]) => featureToggle.enabled) ?? defaultValue
}
