import React, { ReactElement, useState } from 'react'
import { ApiError, ApiResponse } from '../api/apiClient'

export function useApiCall<T, U>(
  fn: (req: T) => Promise<ApiResponse<U>>
): [Result<U>, (args: T, onSuccess?: (result: U) => void, onError?: (error: ApiError) => void) => void, () => void] {
  const [apiResult, setApiResult] = useState<Result<U>>(initial)

  const callFn = React.useCallback(
    async (args: T, onSuccess?: (result: U) => void, onError?: (error: any) => void) => {
      if (!isPending(apiResult)) {
        setApiResult(pending)

        const res = await fn(args)
        if (res.status === 'ok') {
          setApiResult(success(res.data))
          onSuccess?.(res.data)
        } else {
          setApiResult(error(res))
          onError?.(res)
        }
      }
    },
    [apiResult, fn]
  )

  const resetToInitial = React.useCallback(() => {
    setApiResult(initial)
  }, [setApiResult])

  return [apiResult, callFn, resetToInitial]
}

export type Result<T> = Initial | Pending | Error<ApiError> | Success<T>

type Initial = { status: 'initial' }
type Pending = { status: 'pending' }
type Error<U> = { status: 'error'; error: U }
type Success<T> = { status: 'success'; data: T }

export const isPending = (result: Result<unknown>): result is Pending => result.status === 'pending'
export const isSuccess = <T>(result: Result<T>): result is Success<T> => result.status === 'success'
export const isFailure = (result: Result<unknown>): result is Error<ApiError> => result.status === 'error'
export const isConflict = (result: Result<unknown>): result is Error<ApiError> =>
  result.status === 'error' && result.error.statusCode == '409'
export const isInitial = (result: Result<unknown>): result is Initial => result.status === 'initial'
export const isPendingOrInitial = (result: Result<unknown>): result is Initial | Pending =>
  isPending(result) || isInitial(result)

const initial = <A = never>(): Result<A> => ({ status: 'initial' })
const pending = <A = never>(): Result<A> => ({ status: 'pending' })
const success = <A = never>(data: A): Result<A> => ({
  status: 'success',
  data,
})
const error = <A = never>(error: ApiError): Result<A> => ({
  status: 'error',
  error,
})

export const mapApiResult = <T>(
  result: Result<T>,
  mapInitialOrPending: ReactElement,
  mapError: (_: ApiError) => ReactElement,
  mapSuccess: (_: T) => ReactElement
): ReactElement => {
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
