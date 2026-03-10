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

  if (sanksjoner.length > 0) {
    return (
      <>
        {sanksjoner.map((sanksjon) => {
          if (!sanksjon.tom) {
            return (
              <Tag key={sanksjon.id} variant="warning">
                Midlertidig stans fra {formaterDato(sanksjon.fom)}
              </Tag>
            )
          }
          if (new Date(sanksjon.tom) >= new Date()) {
            return (
              <Tag key={sanksjon.id} variant="warning">
                Midlertidig stans fra og med {formaterDato(sanksjon.fom)} til og med {formaterDato(sanksjon.tom)}
              </Tag>
            )
          }
          return null
        })}
      </>
    )
  }
}
