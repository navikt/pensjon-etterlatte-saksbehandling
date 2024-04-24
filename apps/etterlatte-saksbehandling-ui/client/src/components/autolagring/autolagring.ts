import { debounce } from 'lodash'
import { useCallback, useEffect, useState } from 'react'
import { UseFormReturn, useWatch } from 'react-hook-form'
import useDeepCompareEffect from 'use-deep-compare-effect'

interface Props<T> {
  defaultValues: T
  methods: UseFormReturn<any>
  onSubmit: (data: T) => void
}
export const useAutolagring = <T>({ defaultValues, methods, onSubmit }: Props<T>) => {
  const [lagret, settLagret] = useState<boolean>(false)

  /**
   * For å hindre at det blir lagret ved hver isDirty, lagres det ikke på nytt
   * før det har gått et stykke tid */
  const debouncedSave = useCallback(
    debounce(() => {
      methods.handleSubmit(onSubmit)()
    }, 2000),
    []
  )

  const watchedData = useWatch({
    control: methods.control,
    defaultValue: defaultValues,
  })

  /**
   * Bruker useDeepCompareEffect for å sikre at alle felter i
   * hele objekttreet trigger en lagring
   */
  useDeepCompareEffect(() => {
    console.log('useDeep: ', methods.formState.isDirty)
    if (methods.formState.isDirty) {
      settLagret(false)
      debouncedSave()
    }
  }, [watchedData])

  useEffect(() => {
    return () => {
      debouncedSave.cancel()
    }
  }, [debouncedSave])

  return {
    settLagret,
    lagret,
  }
}
