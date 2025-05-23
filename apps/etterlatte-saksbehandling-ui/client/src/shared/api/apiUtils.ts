import { ApiError } from '~shared/api/apiClient'
import { ReactElement } from 'react'

export type Result<T> = Initial | Pending | Error<ApiError> | Success<T>
type Initial = { status: 'initial' }
type Pending = { status: 'pending' }
type Error<U> = { status: 'error'; error: U }
type Success<T> = { status: 'success'; data: T }

export const isPending = (result: Result<unknown>): result is Pending => result.status === 'pending'
export const isSuccess = <T>(result: Result<T>): result is Success<T> => result.status === 'success'
export const isFailure = (result: Result<unknown>): result is Error<ApiError> => result.status === 'error'
export const isInitial = (result: Result<unknown>): result is Initial => result.status === 'initial'
export const isPendingOrInitial = (result: Result<unknown>): result is Initial | Pending =>
  isPending(result) || isInitial(result)
export const isSuccessOrInitial = (result: Result<unknown>): result is Initial | Success<unknown> =>
  isSuccess(result) || isInitial(result)

export type Mappers<T, R> = {
  success?: (_: T) => R
  initial?: R
  pending?: R
  error?: ((_: ApiError) => R) | R
}

export const mapResultFallback = <T, R, F>(result: Result<T>, mappers: Mappers<T, R>, fallback: F): R | F => {
  if (isInitial(result)) {
    return mappers.initial !== undefined ? mappers.initial : fallback
  }
  if (isPending(result)) {
    return mappers.pending !== undefined ? mappers.pending : fallback
  }
  if (isFailure(result)) {
    if (!mappers.error) {
      return fallback
    }
    if (typeof mappers.error === 'function') {
      return (mappers.error as (_: ApiError) => R)(result.error)
    } else {
      return mappers.error
    }
  }
  if (isSuccess(result)) {
    return mappers.success ? mappers.success(result.data) : fallback
  }
  throw new Error(`Ukjent status på result: ${JSON.stringify(result)}`)
}

/**
 * Mapper om fra en resultatType til en annen. Kan brukes til å transformere innholdet i et resultat men samtidig
 * beholde all informasjon om tilstanden til resultatet.
 *
 * @param result av type T
 * @param transform som mapper fra T til R
 *
 * @return Result av typen R
 */
export const transformResult = <T, R>(result: Result<T>, transform: (_: T) => R): Result<R> =>
  mapResultFallback(
    result,
    {
      success: (data: T): Result<R> => ({
        status: 'success',
        data: transform(data),
      }),
    },
    // Denne casten er trygg siden det er kun success som har objekt med typeinformasjon, og denne mapper vi over
    result as Result<R>
  )

export const mapResult = <T, R>(result: Result<T>, mappers: Mappers<T, R>) => mapResultFallback(result, mappers, null)

//TODO: ikke bruk, burde slettes og skrives om til mapResult over
export const mapApiResult = <T>(
  result: Result<T>,
  mapInitialOrPending: ReactElement,
  mapError: (_: ApiError) => ReactElement | null,
  mapSuccess: (_: T) => ReactElement
): ReactElement | null => {
  return mapResult(result, {
    pending: mapInitialOrPending,
    initial: mapInitialOrPending,
    error: (error) => (error.status === 502 ? null : mapError(error)),
    success: mapSuccess,
  })
}

export const mapSuccess = <T, R>(result: Result<T>, mapSuccess: (success: T) => R): R | null =>
  mapResult(result, { success: mapSuccess })

export const mapFailure = <T, R>(result: Result<T>, mapFailure: (error: ApiError) => R): R | null =>
  mapResult(result, { error: mapFailure })
