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
export const isErrorWithCode = (result: Result<unknown>, code: number): result is Error<ApiError> =>
  result.status === 'error' && result.error.status === code

export const initial = <A = never>(): Result<A> => ({ status: 'initial' })
export const pending = <A = never>(): Result<A> => ({ status: 'pending' })
export const success = <A = never>(data: A): Result<A> => ({
  status: 'success',
  data,
})
export const error = <A = never>(error: ApiError): Result<A> => ({
  status: 'error',
  error,
})
export const mapApiResult = <T>(
  result: Result<T>,
  mapInitialOrPending: ReactElement,
  mapError: (_: ApiError) => ReactElement | null,
  mapSuccess: (_: T) => ReactElement
): ReactElement | null => {
  if (isPendingOrInitial(result)) {
    return mapInitialOrPending
  }
  if (isFailure(result)) {
    return mapError(result.error)
  }
  if (isSuccess(result)) {
    return mapSuccess(result.data)
  }
  throw new Error(`Unknown state of result: ${JSON.stringify(result)}`)
}
export const mapAllApiResult = <T>(
  result: Result<T>,
  mapPending: ReactElement,
  mapInitial: ReactElement | null,
  mapError: (_: ApiError) => ReactElement,
  mapSuccess: (_: T) => ReactElement | null
): ReactElement | null => {
  if (isPending(result)) {
    return mapPending
  }
  if (isInitial(result)) {
    return mapInitial
  }
  if (isFailure(result)) {
    return mapError(result.error)
  }
  if (isSuccess(result)) {
    return mapSuccess(result.data)
  }
  throw new Error(`Unknown state of result: ${JSON.stringify(result)}`)
}
export const mapSuccess = <T, R>(result: Result<T>, mapSuccess: (success: T) => R): R | null => {
  if (isSuccess(result)) {
    return mapSuccess(result.data)
  }
  return null
}
