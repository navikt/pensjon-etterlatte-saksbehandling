import React, { ReactNode, useEffect } from 'react'
import { SpaceChildren } from '~shared/styled'
import { Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakType } from '~components/vedtak/typer'
import { formaterStringDato } from '~utils/formattering'
import { VedtaketKlagenGjelder } from '~shared/types/Klage'
import { RecordFillIcon, XMarkIcon } from '@navikt/aksel-icons'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [vedtakISakResult, vedtakISakFetch] = useApiCall(hentAlleVedtakISak)

  const visStatusPaaSisteVedtak = (vedtakISak: VedtaketKlagenGjelder[]): ReactNode => {
    const sisteVedtak = vedtakISak
      .filter((vedtak) => ![VedtakType.AVVIST_KLAGE, VedtakType.TILBAKEKREVING].includes(vedtak.vedtakType!))
      .filter((vedtak) => !!vedtak.datoAttestert)
      .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
      .pop()

    switch (sisteVedtak?.vedtakType) {
      case VedtakType.INNVILGELSE:
        return (
          <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
            Løpende fra {!!sisteVedtak.datoAttestert && formaterStringDato(sisteVedtak.datoAttestert)}
          </Tag>
        )
      case VedtakType.AVSLAG:
        return (
          <Tag key={VedtakType.AVSLAG} variant="error" icon={<XMarkIcon aria-hidden color="#C30000" />}>
            Avslått den {!!sisteVedtak.datoAttestert && formaterStringDato(sisteVedtak.datoAttestert)}
          </Tag>
        )
      case VedtakType.OPPHOER:
        return (
          <Tag key={VedtakType.OPPHOER} variant="alt2">
            Ytelse opphørte den {!!sisteVedtak.datoAttestert && formaterStringDato(sisteVedtak.datoAttestert)}
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
    vedtakISakFetch(sakId)
  }, [])

  return (
    <SpaceChildren direction="row">
      {mapResult(vedtakISakResult, {
        pending: <Loader />,
        error: <Tag variant="error">Kunne ikke hente status</Tag>,
        success: (vedtakISak) =>
          !!vedtakISak?.length ? visStatusPaaSisteVedtak(vedtakISak) : <Tag variant="neutral">Ingen vedtak på sak</Tag>,
      })}
    </SpaceChildren>
  )
}
