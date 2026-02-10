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
        <BodyShort>
          <AktivitetspliktStatusTag status={finnStatusPaaAktivitetsplikt(aktivitetspliktVurdering)} />
        </BodyShort>
      ) : (
        <BodyShort>Ingen vurdering</BodyShort>
      )}
    </>
  )
}

export const AktivitetspliktStatusTag = ({ status }: { status: StatusPaaAktivitetsplikt }): ReactNode => {
  switch (status.aktivitetspliktStatus) {
    case AktivitetspliktStatus.OPPFYLT:
      return (
        <Tag data-color="success" variant="outline">
          Aktivitetsplikt oppfylt fra {formaterDatoMedFallback(status.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.IKKE_OPPFYLT:
      return (
        <Tag data-color="danger" variant="outline">
          Aktivitetsplikt ikke oppfylt fra {formaterDatoMedFallback(status.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.UNNTAK:
      return (
        <Tag data-color="meta-lime" variant="outline">
          Unntak fra aktivitetskrav til {formaterDatoMedFallback(status.dato, '-')}
        </Tag>
      )
    case AktivitetspliktStatus.VARIG_UNNTAK:
      return (
        <Tag data-color="info" variant="outline">
          Varig unntak fra aktivitetsplikt
        </Tag>
      )
    case AktivitetspliktStatus.IKKE_VURDERT:
      return (
        <Tag data-color="neutral" variant="outline">
          Aktivitetskrav ikke vurdert
        </Tag>
      )
    default:
      return (
        <Tag data-color="neutral" variant="outline">
          Ingen status på aktivitet
        </Tag>
      )
  }
}
