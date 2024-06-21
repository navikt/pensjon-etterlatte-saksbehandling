import { useParams } from 'react-router-dom'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { dekrypter } from '~shared/api/krypter'
import { mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { fnrHarGyldigFormat } from '~utils/fnr'

export const DekrypterendeWrapper = ({ component }: { component: (fnr: string) => JSX.Element }) => {
  const { fnr } = useParams()

  const [dekryptert, dekrypterFetch] = useApiCall(dekrypter)

  useEffect(() => {
    if (fnr) {
      dekrypterFetch(fnr!!)
    }
  }, [fnr])

  return mapResult(dekryptert, {
    success: (res) => {
      if (!fnrHarGyldigFormat(res.respons)) {
        return <ApiErrorAlert>Fødselsnummeret {res.respons} har et ugyldig format (ikke 11 siffer)</ApiErrorAlert>
      }
      return component(res.respons)
    },
    error: (error) => {
      if (error.status === 400) {
        return <ApiErrorAlert>Ugyldig forespørsel: {error.detail}</ApiErrorAlert>
      } else {
        return <ApiErrorAlert>Feil oppsto ved henting av person med fødselsnummer {fnr}</ApiErrorAlert>
      }
    },
  })
}
