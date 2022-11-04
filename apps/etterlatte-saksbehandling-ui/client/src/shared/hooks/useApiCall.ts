import React, { useState } from 'react'
import { ApiError, ApiResponse } from '../api/apiClient'

export function useApiCall<T, U>(
  fn: (req: T) => Promise<ApiResponse<U>>
): [Result<U>, (args: T, onSuccess?: (result: U) => void) => void, () => void] {
  const [apiResult, setApiResult] = useState<Result<U>>(initial)

  const callFn = React.useCallback(
    async (args: T, onSuccess?: (result: U) => void) => {
      if (!isPending(apiResult)) {
        setApiResult(pending)

        const res = await fn(args)
        if (res.status === 'ok') {
          setApiResult(success(res.data))
          onSuccess?.(res.data)
        } else {
          setApiResult(error(res))
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
export const isInitial = (result: Result<unknown>): result is Initial => result.status === 'initial'

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
