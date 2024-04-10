import React, { ReactNode, useEffect } from 'react'
import { SpaceChildren } from '~shared/styled'
import { Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentIverksatteVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [iverksatteVedtakResult, iverksatteVedtakFetch] = useApiCall(hentIverksatteVedtakISak)

  const visStatusPaaSisteVedtak = (iverksatteVedtak: VedtakSammendrag[]): ReactNode => {
    const sisteIversatteVedtak = [...iverksatteVedtak].pop()

    switch (sisteIversatteVedtak?.vedtakType) {
      case VedtakType.INNVILGELSE:
        return (
          <Tag key={VedtakType.INNVILGELSE} variant="success">
            Løpende
          </Tag>
        )
      case VedtakType.AVSLAG:
        return (
          <Tag key={VedtakType.AVSLAG} variant="error">
            Avslått
          </Tag>
        )
      case VedtakType.OPPHOER:
        return (
          <Tag key={VedtakType.AVSLAG} variant="alt2">
            Ytelse opphørt
          </Tag>
        )
      default:
        return (
          <Tag key="annen-type" variant="warning">
            Ubehandlet
          </Tag>
        )
    }
  }

  useEffect(() => {
    iverksatteVedtakFetch(sakId)
  }, [])

  return (
    <SpaceChildren direction="row">
      {mapResult(iverksatteVedtakResult, {
        pending: <Loader />,
        error: <Tag variant="error">Kunne ikke hente status</Tag>,
        success: (iverksatteVedtak) =>
          !!iverksatteVedtak?.length ? (
            visStatusPaaSisteVedtak(iverksatteVedtak)
          ) : (
            <Tag variant="neutral">Ingen vedtak på sak</Tag>
          ),
      })}
    </SpaceChildren>
  )
}
