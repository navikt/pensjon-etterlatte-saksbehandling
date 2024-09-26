import React, { ReactNode } from 'react'
import { IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
import { Tag } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { AktivitetspliktStatus, finnStatusPaaAktivitetsplikt } from '~components/person/aktivitet/utils'

export const AktivitetspliktStatusTag = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNy
}): ReactNode => {
  const statusPaaAktivitetsplikt = finnStatusPaaAktivitetsplikt(aktivitetspliktVurdering)

  switch (statusPaaAktivitetsplikt.aktivitetspliktStatus) {
    case AktivitetspliktStatus.OPPFYLT:
      return (
        <Tag variant="success">
          Aktivitetsplikt oppfylt fra {formaterDatoMedFallback(statusPaaAktivitetsplikt.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.IKKE_OPPFYLT:
      return (
        <Tag variant="error">
          Aktivitetsplikt oppfylt fra {formaterDatoMedFallback(statusPaaAktivitetsplikt.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.UNNTAK:
      return (
        <Tag variant="alt2">
          Unntak fra aktivitetskrav til {formaterDatoMedFallback(statusPaaAktivitetsplikt.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.VARIG_UNNTAK:
      return <Tag variant="info">Varig unntak fra aktivitetsplikt</Tag>
    case AktivitetspliktStatus.IKKE_VURDERT:
      return <Tag variant="neutral">Aktivitetskrav ikke vurdert</Tag>
    default:
      return <Tag variant="neutral">Ingen status på aktivitet</Tag>
  }
}
