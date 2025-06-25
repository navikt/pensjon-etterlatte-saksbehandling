import { AdressebeskyttelseGradering } from '~shared/types/sak'
import { Tag } from '@navikt/ds-react'
import { HouseIcon } from '@navikt/aksel-icons'

export const AdressebeskyttelseGraderingTag = ({
  adressebeskyttelse,
  size,
}: {
  adressebeskyttelse: AdressebeskyttelseGradering
  size?: 'medium' | 'small' | 'xsmall'
}) => {
  // Viser ingenting hvis adressebeskytelse er satt til ugradert
  switch (adressebeskyttelse) {
    case AdressebeskyttelseGradering.FORTROLIG:
      return (
        <Tag variant="warning" size={size} icon={<HouseIcon aria-hidden />}>
          Fortrolig adresse
        </Tag>
      )
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG:
      return (
        <Tag variant="error" size={size} icon={<HouseIcon aria-hidden />}>
          Strengt fortrolig adresse
        </Tag>
      )
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND:
      return (
        <Tag variant="error" size={size} icon={<HouseIcon aria-hidden />}>
          Strengt fortrolig utland adresse
        </Tag>
      )
  }
}
