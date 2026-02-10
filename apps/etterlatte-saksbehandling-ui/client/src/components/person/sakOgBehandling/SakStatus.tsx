import React, { ReactNode, useEffect } from 'react'
import { HStack, Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'
import { formaterDato } from '~utils/formatering/dato'
import { RecordFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import {
  hentInnvilgelseVedtak,
  hentLoependeVedtak,
  ytelseErLoependeMedOpphoerFremITid,
  ytelseErOpphoert,
  ytelseOpphoersdato,
} from '~components/person/sakOgBehandling/sakStatusUtils'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [vedtakISakResult, vedtakISakFetch] = useApiCall(hentAlleVedtakISak)

  const visStatusPaaSisteVedtak = (vedtakISak: VedtakSammendrag[]): ReactNode => {
    const loependeVedtak = hentLoependeVedtak(vedtakISak)
    const innvilgelsesVedtak = hentInnvilgelseVedtak(vedtakISak)

    if (!loependeVedtak) {
      return (
        <Tag key="annen-type" variant="warning">
          Ubehandlet
        </Tag>
      )
    }

    if (loependeVedtak?.vedtakType === VedtakType.AVSLAG) {
      return (
        <Tag key={VedtakType.AVSLAG} variant="error" icon={<XMarkIcon aria-hidden color="#C30000" />}>
          Avslått den {loependeVedtak.datoAttestert && formaterDato(loependeVedtak.datoAttestert)}
        </Tag>
      )
    }

    if (ytelseErOpphoert(loependeVedtak)) {
      const opphoerer = ytelseOpphoersdato(loependeVedtak)
      return (
        <Tag key={VedtakType.OPPHOER} variant="alt2">
          Opphørt fra {opphoerer && formaterDato(opphoerer)}
        </Tag>
      )
    }

    if (ytelseErLoependeMedOpphoerFremITid(loependeVedtak)) {
      return (
        <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
          Løpende til {loependeVedtak.opphoerFraOgMed && formaterDato(loependeVedtak.opphoerFraOgMed)}
        </Tag>
      )
    }

    return (
      <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
        Løpende fra {innvilgelsesVedtak?.virkningstidspunkt && formaterDato(innvilgelsesVedtak.virkningstidspunkt)}
      </Tag>
    )
  }

  useEffect(() => {
    vedtakISakFetch(sakId)
  }, [])

  return (
    <HStack gap="space-4">
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
