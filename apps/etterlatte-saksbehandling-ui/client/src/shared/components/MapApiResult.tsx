import { mapApiResult, Result } from '~shared/api/apiUtils'
import { ReactElement } from 'react'
import { ApiError } from '~shared/api/apiClient'

interface MapApiResultProps<T> {
  result: Result<T>
  mapInitialOrPending: ReactElement
  mapError: (_: ApiError) => ReactElement
  mapSuccess: (_: T) => ReactElement
}

export const MapApiResult = <T,>(props: MapApiResultProps<T>) => {
  return mapApiResult(props.result, props.mapInitialOrPending, props.mapError, props.mapSuccess)
}
