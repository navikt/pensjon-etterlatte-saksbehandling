import React, { ReactNode } from 'react'
import {
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNy,
} from '~shared/types/Aktivitetsplikt'
import { Tag } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

enum AktivitetspliktStatus {
  OPPFYLT,
  IKKE_OPPFYLT,
  UNNTAK,
  VARIG_UNNTAK,
  IKKE_VURDERT,
}

export const AktivitetspliktStatusTag = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNy
}): ReactNode => {
  const finnStatusPaaAktivitetsplikt = (
    aktivitetspliktVurdering: IAktivitetspliktVurderingNy
  ): { aktivitetspliktStatus: AktivitetspliktStatus; dato?: Date | string } => {
    if (!!aktivitetspliktVurdering.unntak?.length) {
      const gjeldendeUnntak: IAktivitetspliktUnntak = [...aktivitetspliktVurdering.unntak].reduce((a, b) => {
        return new Date(a.fom) > new Date(b.fom) ? a : b
      })

      if (gjeldendeUnntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT) {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.VARIG_UNNTAK,
        }
      } else {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.UNNTAK,
          dato: gjeldendeUnntak?.tom,
        }
      }
    } else if (!!aktivitetspliktVurdering.aktivitet?.length) {
      const gjeldendeAktivitet = [...aktivitetspliktVurdering.aktivitet].reduce((a, b) => {
        return new Date(a.fom) > new Date(b.fom) ? a : b
      })

      if (gjeldendeAktivitet?.aktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50) {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.IKKE_OPPFYLT,
          dato: gjeldendeAktivitet.fom,
        }
      } else {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.OPPFYLT,
          dato: gjeldendeAktivitet?.fom,
        }
      }
    }

    return {
      aktivitetspliktStatus: AktivitetspliktStatus.IKKE_VURDERT,
    }
  }

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
      return <Tag variant="info">Varig unntakt fra aktivitets(plikt/krav)</Tag>
    case AktivitetspliktStatus.IKKE_VURDERT:
      return <Tag variant="neutral">Aktivitetskrav ikke vurdert</Tag>
    default:
      return <Tag variant="neutral">Ingen status på aktivitet</Tag>
  }
}
