import React, { ReactNode } from 'react'
import { harVurdering, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Label, Tag } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import {
  AktivitetspliktStatus,
  finnStatusPaaAktivitetsplikt,
  StatusPaaAktivitetsplikt,
} from '~components/person/aktivitet/utils'

export const AktivitetspliktStatusTagOgGyldig = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNyDto
}) => {
  return (
    <>
      <Label>Status på gjenlevende sin aktivitet</Label>
      {harVurdering(aktivitetspliktVurdering) ? (
        <div>
          <AktivitetspliktStatusTag status={finnStatusPaaAktivitetsplikt(aktivitetspliktVurdering)} />
        </div>
      ) : (
        <BodyShort>Ingen vurdering</BodyShort>
      )}
    </>
  )
}

export const AktivitetspliktStatusTag = ({ status }: { status: StatusPaaAktivitetsplikt }): ReactNode => {
  switch (status.aktivitetspliktStatus) {
    case AktivitetspliktStatus.OPPFYLT:
      return <Tag variant="success">Aktivitetsplikt oppfylt fra {formaterDatoMedFallback(status.dato, '-')}</Tag>
    case AktivitetspliktStatus.IKKE_OPPFYLT:
      return <Tag variant="error">Aktivitetsplikt oppfylt fra {formaterDatoMedFallback(status.dato, '-')}</Tag>
    case AktivitetspliktStatus.UNNTAK:
      return <Tag variant="alt2">Unntak fra aktivitetskrav til {formaterDatoMedFallback(status.dato, '-')}</Tag>
    case AktivitetspliktStatus.VARIG_UNNTAK:
      return <Tag variant="info">Varig unntak fra aktivitetsplikt</Tag>
    case AktivitetspliktStatus.IKKE_VURDERT:
      return <Tag variant="neutral">Aktivitetskrav ikke vurdert</Tag>
    default:
      return <Tag variant="neutral">Ingen status på aktivitet</Tag>
  }
}
