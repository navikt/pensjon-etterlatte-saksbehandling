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
        <Tag data-color="info" variant="moderate" size={size}>
          Nasjonal
        </Tag>
      )
    case UtlandstilknytningType.BOSATT_UTLAND:
      return (
        <Tag data-color="warning" variant="moderate" size={size}>
          Bosatt utland
        </Tag>
      )
    case UtlandstilknytningType.UTLANDSTILSNITT:
      return (
        <Tag data-color="warning" variant="strong" size={size}>
          Utlandstilsnitt
        </Tag>
      )
    default:
      return (
        <Tag data-color="danger" variant="strong" size={size}>
          Ingen tilknytning
        </Tag>
      )
  }
}
