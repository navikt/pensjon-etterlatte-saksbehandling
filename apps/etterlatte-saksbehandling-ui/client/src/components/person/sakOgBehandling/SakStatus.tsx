import React, { ReactNode, useEffect } from 'react'
import { SpaceChildren } from '~shared/styled'
import { Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentIverksatteVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'
import { formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [iverksatteVedtakResult, iverksatteVedtakFetch] = useApiCall(hentIverksatteVedtakISak)

  const visStatusPaaSisteVedtak = (iverksatteVedtak: VedtakSammendrag[]): ReactNode => {
    const sisteIversatteVedtak = [...iverksatteVedtak].pop()

    switch (sisteIversatteVedtak?.vedtakType) {
      case VedtakType.INNVILGELSE:
        return (
          <SpaceChildren direction="row">
            <Tag variant="neutral">
              {formaterEnumTilLesbarString(sisteIversatteVedtak.vedtakType)}{' '}
              {!!sisteIversatteVedtak.datoFattet && formaterStringDato(sisteIversatteVedtak.datoFattet)}
            </Tag>
            <Tag key={VedtakType.INNVILGELSE} variant="success">
              Løpende
            </Tag>
          </SpaceChildren>
        )
      case VedtakType.AVSLAG:
        return (
          <SpaceChildren direction="row">
            <Tag variant="neutral">
              {formaterEnumTilLesbarString(sisteIversatteVedtak.vedtakType)}{' '}
              {!!sisteIversatteVedtak.datoFattet && formaterStringDato(sisteIversatteVedtak.datoFattet)}
            </Tag>
            <Tag key={VedtakType.AVSLAG} variant="error">
              Avslått
            </Tag>
          </SpaceChildren>
        )
      case VedtakType.OPPHOER:
        return (
          <SpaceChildren direction="row">
            <Tag variant="neutral">
              {formaterEnumTilLesbarString(sisteIversatteVedtak.vedtakType)}{' '}
              {!!sisteIversatteVedtak.datoFattet && formaterStringDato(sisteIversatteVedtak.datoFattet)}
            </Tag>
            <Tag key={VedtakType.AVSLAG} variant="alt2">
              Ytelse opphørt
            </Tag>
          </SpaceChildren>
        )
      default:
        return (
          <SpaceChildren direction="row">
            <Tag variant="neutral">
              <>
                {!!sisteIversatteVedtak && !!sisteIversatteVedtak.vedtakType && (
                  <>
                    {formaterEnumTilLesbarString(sisteIversatteVedtak.vedtakType)}{' '}
                    {sisteIversatteVedtak.datoFattet && formaterStringDato(sisteIversatteVedtak.datoFattet)}
                  </>
                )}
              </>
            </Tag>
            <Tag key="annen-type" variant="warning">
              Ubehandlet
            </Tag>
          </SpaceChildren>
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
