import React, { ReactNode, useEffect } from 'react'
import { SpaceChildren } from '~shared/styled'
import { Loader, Tag } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'
import { formaterStringDato } from '~utils/formattering'
import { RecordFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import { hentFoersteVedtak, hentSisteVedtak } from '~components/person/sakOgBehandling/sakStatusUtils'

export const SakStatus = ({ sakId }: { sakId: number }) => {
  const [vedtakISakResult, vedtakISakFetch] = useApiCall(hentAlleVedtakISak)

  const visStatusPaaSisteVedtak = (vedtakISak: VedtakSammendrag[]): ReactNode => {
    const sisteVedtak = hentSisteVedtak(vedtakISak)

    if (!sisteVedtak) {
      return (
        <Tag key="annen-type" variant="warning">
          Ubehandlet
        </Tag>
      )
    }

    if (sisteVedtak?.vedtakType === VedtakType.AVSLAG) {
      return (
        <Tag key={VedtakType.AVSLAG} variant="error" icon={<XMarkIcon aria-hidden color="#C30000" />}>
          Avslått den {sisteVedtak.virkningstidspunkt && formaterStringDato(sisteVedtak.virkningstidspunkt)}
        </Tag>
      )
    }

    const iDag = new Date()
    const opphoerFraOgMed = sisteVedtak?.opphoerFraOgMed ? new Date(sisteVedtak.opphoerFraOgMed) : undefined

    if (sisteVedtak?.vedtakType === VedtakType.OPPHOER || (opphoerFraOgMed && opphoerFraOgMed < iDag)) {
      // TODO opphoerFraOgMed er nytt felt slik at opphørsvedtak som fantes før feltet vil ikke dette feltet populert
      // Det skal populeres og når det er gjort kan vi anta her at et opphør alltid vil ha opphoerFraOgMed
      const opphoerer = sisteVedtak.opphoerFraOgMed ? sisteVedtak.opphoerFraOgMed : sisteVedtak.virkningstidspunkt
      return (
        <Tag key={VedtakType.OPPHOER} variant="alt2">
          Ytelse opphørte den {opphoerer && formaterStringDato(opphoerer)}
        </Tag>
      )
    }

    const foersteVedtak = hentFoersteVedtak(vedtakISak)

    if (opphoerFraOgMed && opphoerFraOgMed >= iDag) {
      return (
        <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
          Løpende fra {foersteVedtak?.virkningstidspunkt && formaterStringDato(foersteVedtak.virkningstidspunkt)} og
          opphører {sisteVedtak.opphoerFraOgMed && formaterStringDato(sisteVedtak.opphoerFraOgMed)}
        </Tag>
      )
    }

    return (
      <Tag key={VedtakType.INNVILGELSE} variant="success" icon={<RecordFillIcon aria-hidden color="#06893A" />}>
        Løpende fra {foersteVedtak?.virkningstidspunkt && formaterStringDato(foersteVedtak.virkningstidspunkt)}
      </Tag>
    )
  }

  useEffect(() => {
    vedtakISakFetch(sakId)
  }, [])

  return (
    <SpaceChildren direction="row">
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
    </SpaceChildren>
  )
}
