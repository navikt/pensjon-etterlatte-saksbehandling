import React, { ReactNode, useEffect } from 'react'
import { HStack, Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakType } from '~components/vedtak/typer'
import { formaterStringDato } from '~utils/formattering'
import { VedtaketKlagenGjelder } from '~shared/types/Klage'
import { RecordFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import { hentStatusPaaSak } from '~components/person/sakOgBehandling/sakStatusUtils'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [vedtakISakResult, vedtakISakFetch] = useApiCall(hentAlleVedtakISak)

  const visStatusPaaSisteVedtak = (vedtakISak: VedtaketKlagenGjelder[]): ReactNode => {
    const sisteVedtak = hentStatusPaaSak(vedtakISak)

    switch (sisteVedtak?.vedtakType) {
      case VedtakType.INNVILGELSE:
        return (
          <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
            Løpende fra {!!sisteVedtak.virkningstidspunkt && formaterStringDato(sisteVedtak.virkningstidspunkt)}
          </Tag>
        )
      case VedtakType.AVSLAG:
        return (
          <Tag key={VedtakType.AVSLAG} variant="error" icon={<XMarkIcon aria-hidden color="#C30000" />}>
            Avslått den {!!sisteVedtak.virkningstidspunkt && formaterStringDato(sisteVedtak.virkningstidspunkt)}
          </Tag>
        )
      case VedtakType.OPPHOER:
        return (
          <Tag key={VedtakType.OPPHOER} variant="alt2">
            Ytelse opphørte den {!!sisteVedtak.virkningstidspunkt && formaterStringDato(sisteVedtak.virkningstidspunkt)}
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
    <HStack gap="4">
      {mapResult(vedtakISakResult, {
        pending: <Loader />,
        error: <Tag variant="error">Kunne ikke hente status</Tag>,
        success: (vedtakISak) => {
          return !!vedtakISak?.length ? (
            visStatusPaaSisteVedtak(vedtakISak)
          ) : (
            <Tag variant="neutral">Ingen vedtak på sak</Tag>
          )
        },
      })}
    </HStack>
  )
}
