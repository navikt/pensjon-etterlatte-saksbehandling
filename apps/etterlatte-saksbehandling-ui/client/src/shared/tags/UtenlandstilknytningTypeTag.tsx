import React from 'react'
import { UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { Tag } from '@navikt/ds-react'

export const UtenlandstilknytningTypeTag = ({
  utenlandstilknytningType,
  size,
}: {
  utenlandstilknytningType: UtlandstilknytningType | null | undefined
  size?: 'xsmall' | 'small' | 'medium'
}) => {
  switch (utenlandstilknytningType) {
    case UtlandstilknytningType.NASJONAL:
      return (
        <Tag variant="info-moderate" size={size}>
          Nasjonal
        </Tag>
      )
    case UtlandstilknytningType.BOSATT_UTLAND:
      return (
        <Tag variant="warning-moderate" size={size}>
          Bosatt utland
        </Tag>
      )
    case UtlandstilknytningType.UTLANDSTILSNITT:
      return (
        <Tag variant="warning-filled" size={size}>
          Utlandstilsnitt
        </Tag>
      )
    default:
      return (
        <Tag variant="error-filled" size={size}>
          Ingen tilknytning
        </Tag>
      )
  }
}
