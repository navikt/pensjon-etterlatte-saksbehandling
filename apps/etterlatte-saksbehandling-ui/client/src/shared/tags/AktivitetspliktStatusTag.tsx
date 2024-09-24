import React, { ReactNode } from 'react'
import { AktivitetspliktVurderingType, IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
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
      const unntak = [...aktivitetspliktVurdering.unntak].pop()
      // TODO: sjekke om dette faktisk er riktig?
      if (!!unntak?.tom) {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.VARIG_UNNTAK,
        }
      } else {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.UNNTAK,
          dato: unntak?.tom,
        }
      }
    } else if (!!aktivitetspliktVurdering.aktivitet?.length) {
      const aktivitetskrav = [...aktivitetspliktVurdering.aktivitet].pop()
      if (aktivitetskrav?.aktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50) {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.IKKE_OPPFYLT,
          dato: aktivitetskrav.fom,
        }
      } else {
        return {
          aktivitetspliktStatus: AktivitetspliktStatus.OPPFYLT,
          dato: aktivitetskrav?.fom,
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
