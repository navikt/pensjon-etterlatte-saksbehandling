import { AdressebeskyttelseGradering } from '~shared/types/sak'
import { Tag } from '@navikt/ds-react'

export const AdressebeskyttelseGraderingTag = ({
  adressebeskyttelse,
}: {
  adressebeskyttelse: AdressebeskyttelseGradering
}) => {
  switch (adressebeskyttelse) {
    case AdressebeskyttelseGradering.UGRADERT:
      return <Tag variant="neutral">Ugradert</Tag>
    case AdressebeskyttelseGradering.FORTROLIG:
      return <Tag variant="warning">Fortrolig</Tag>
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG:
      return <Tag variant="error">Strengt fortrolig</Tag>
    case AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND:
      return <Tag variant="error">Strengt fortrolig utland</Tag>
  }
}
