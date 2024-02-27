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
export const isFailureWithCode = (result: Result<unknown>, code: number): result is Error<ApiError> =>
  result.status === 'error' && result.error.status === code

export type Mappers<T, R> = {
  success?: (_: T) => R
  initial?: R
  pending?: R
  error?: (_: ApiError) => R
}

export const mapResult = <T, R>(result: Result<T>, mappers: Mappers<T, R>): R | null => {
  if (isInitial(result)) {
    return mappers.initial ?? null
  }
  if (isPending(result)) {
    return mappers.pending ?? null
  }
  if (isFailure(result)) {
    return mappers.error?.(result.error) ?? null
  }
  if (isSuccess(result)) {
    return mappers.success?.(result.data) ?? null
  }
  throw new Error(`Ukjent status på result: ${JSON.stringify(result)}`)
}

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

export const mapAllApiResult = <T>(
  result: Result<T>,
  mapPending: ReactElement,
  mapInitial: ReactElement | null,
  mapError: (_: ApiError) => ReactElement,
  mapSuccess: (_: T) => ReactElement | null
): ReactElement | null => {
  return mapResult(result, {
    pending: mapPending,
    initial: mapInitial,
    error: (error) => (error.status === 502 ? null : mapError(error)),
    success: mapSuccess,
  })
}

export const mapSuccess = <T, R>(result: Result<T>, mapSuccess: (success: T) => R): R | null =>
  mapResult(result, { success: mapSuccess })

export const mapFailure = <T, R>(result: Result<T>, mapFailure: (error: ApiError) => R): R | null =>
  mapResult(result, { error: mapFailure })
