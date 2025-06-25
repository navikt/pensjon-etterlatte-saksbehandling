import { AdressebeskyttelseGradering } from '~shared/types/sak'
import { Tag } from '@navikt/ds-react'

export const AdressebeskyttelseGraderingTag = ({
  adressebeskyttelse,
  size,
}: {
  adressebeskyttelse: AdressebeskyttelseGradering
  size?: 'medium' | 'small' | 'xsmall'
}) => {
  switch (adressebeskyttelse) {
    case AdressebeskyttelseGradering.UGRADERT:
      return (
        <Tag variant="neutral" size={size}>
          Ugradert
        </Tag>
      )
    case AdressebeskyttelseGradering.FORTROLIG:
      return (
        <Tag variant="warning" size={size}>
          Fortrolig
        </Tag>
      )
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG:
      return (
        <Tag variant="error" size={size}>
          Strengt fortrolig
        </Tag>
      )
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND:
      return (
        <Tag variant="error" size={size}>
          Strengt fortrolig utland
        </Tag>
      )
  }
}
