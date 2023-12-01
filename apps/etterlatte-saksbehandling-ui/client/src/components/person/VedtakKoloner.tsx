import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import React, { useEffect } from 'react'
import { formaterStringDato, formaterVedtakType } from '~utils/formattering'
import { Table } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ExclamationmarkTriangleFillIcon } from '@navikt/aksel-icons'

import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'

export const VedtakKolonner = (props: { behandlingId: string }) => {
  const [vedtak, apiHentVedtaksammendrag] = useApiCall(hentVedtakSammendrag)

  useEffect(() => {
    apiHentVedtaksammendrag(props.behandlingId)
  }, [])

  const attestertDato = (dato?: string) => {
    if (dato) return formaterStringDato(dato)
    else return ''
  }

  return (
    <>
      {isPending(vedtak) && (
        <Table.DataCell>
          <Spinner visible label="" margin="0" />
        </Table.DataCell>
      )}
      {isSuccess(vedtak) && (
        <>
          <Table.DataCell>{attestertDato(vedtak.data?.datoAttestert)}</Table.DataCell>
          <Table.DataCell>{vedtak.data?.vedtakType && formaterVedtakType(vedtak.data.vedtakType)}</Table.DataCell>
        </>
      )}
      {isFailure(vedtak) && (
        <ExclamationmarkTriangleFillIcon
          title={vedtak.error.detail || 'Feil oppsto ved henting av sammendrag for behandling'}
        />
      )}
    </>
  )
}
