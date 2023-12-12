import { mapAllApiResult, mapApiResult, Result } from '~shared/api/apiUtils'
import { ReactElement } from 'react'
import { ApiError } from '~shared/api/apiClient'

interface MapApiResultProps<T> {
  result: Result<T>
  mapInitialOrPending: ReactElement
  mapError: (_: ApiError) => ReactElement
  mapSuccess: (_: T) => ReactElement
}

interface MapAllApiResultProps<T> {
  result: Result<T>
  mapInitial: ReactElement
  mapPending: ReactElement
  mapError: (_: ApiError) => ReactElement
  mapSuccess: (_: T) => ReactElement
}

export const MapApiResult = <T,>(props: MapApiResultProps<T>) => {
  return mapApiResult(props.result, props.mapInitialOrPending, props.mapError, props.mapSuccess)
}

export const MapAllApiResult = <T,>(props: MapAllApiResultProps<T>) => {
  return mapAllApiResult(props.result, props.mapInitial, props.mapPending, props.mapError, props.mapSuccess)
}
