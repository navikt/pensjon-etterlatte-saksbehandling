import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSanksjonerForSisteIverksatteBehandling } from '~shared/api/sanksjon'
import React, { useEffect } from 'react'
import { Tag } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { isSuccess } from '~shared/api/apiUtils'

interface Props {
  sakId: number
}

export const SanksjonTag = ({ sakId }: Props) => {
  const [sanksjonerResult, sanksjonerFetch] = useApiCall(hentSanksjonerForSisteIverksatteBehandling)
  const sanksjoner = isSuccess(sanksjonerResult) ? sanksjonerResult.data : []

  useEffect(() => {
    sanksjonerFetch(sakId)
  }, [sakId])

  const aktivSanksjon = sanksjoner.find((sanksjon) => {
    return !sanksjon.tom || new Date(sanksjon.tom) >= new Date()
  })

  if (!aktivSanksjon) return null

  return <Tag variant="warning">Midlertidig stans fra {formaterDato(aktivSanksjon.fom)}</Tag>
}
