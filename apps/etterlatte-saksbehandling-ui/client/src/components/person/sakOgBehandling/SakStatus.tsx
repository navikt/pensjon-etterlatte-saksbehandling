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
        <Tag data-color="warning" key="annen-type" variant="outline">
          Ubehandlet
        </Tag>
      )
    }

    if (loependeVedtak?.vedtakType === VedtakType.AVSLAG) {
      return (
        <Tag
          data-color="danger"
          key={VedtakType.AVSLAG}
          variant="outline"
          icon={<XMarkIcon aria-hidden color="#C30000" />}
        >
          Avslått den {loependeVedtak.datoAttestert && formaterDato(loependeVedtak.datoAttestert)}
        </Tag>
      )
    }

    if (ytelseErOpphoert(loependeVedtak)) {
      const opphoerer = ytelseOpphoersdato(loependeVedtak)
      return (
        <Tag data-color="meta-lime" key={VedtakType.OPPHOER} variant="outline">
          Opphørt fra {opphoerer && formaterDato(opphoerer)}
        </Tag>
      )
    }

    if (ytelseErLoependeMedOpphoerFremITid(loependeVedtak)) {
      return (
        <Tag
          data-color="success"
          key={VedtakType.INNVILGELSE}
          variant="outline"
          icon={<RecordFillIcon aria-hidden color="#06893A" />}
        >
          Løpende til {loependeVedtak.opphoerFraOgMed && formaterDato(loependeVedtak.opphoerFraOgMed)}
        </Tag>
      )
    }

    return (
      <Tag
        data-color="success"
        key={VedtakType.INNVILGELSE}
        variant="outline"
        icon={<RecordFillIcon aria-hidden color="#06893A" />}
      >
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
        error: (
          <Tag data-color="danger" variant="outline">
            Kunne ikke hente status
          </Tag>
        ),
        success: (vedtakISak) => {
          return !!vedtakISak?.length ? (
            visStatusPaaSisteVedtak(vedtakISak)
          ) : (
            <Tag data-color="neutral" variant="outline">
              Ingen vedtak på sak
            </Tag>
          )
        },
      })}
    </HStack>
  )
}
