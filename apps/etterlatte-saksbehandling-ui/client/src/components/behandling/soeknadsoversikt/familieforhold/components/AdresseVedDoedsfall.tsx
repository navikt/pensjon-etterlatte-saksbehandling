import { Personopplysning } from '~shared/types/grunnlag'
import { isAfter, isBefore } from 'date-fns'
import { BodyShort } from '@navikt/ds-react'
import { formaterAdresse } from '~shared/types/Person'
import { IAdresse } from '~shared/types/IAdresse'

export const AdresseVedDoedsfall = ({ avdoed }: { avdoed: Personopplysning }) => {
  const adresseVedDoedsfall = (): IAdresse | undefined => {
    if (!!avdoed.opplysning.doedsdato) {
      return avdoed.opplysning.bostedsadresse?.find(
        (adresse) =>
          isAfter(avdoed.opplysning.doedsdato!, adresse.gyldigFraOgMed ?? new Date()) &&
          isBefore(avdoed.opplysning.doedsdato!, adresse.gyldigTilOgMed ?? new Date())
      )
    } else {
      return undefined
    }
  }

  const bostedadresseVedDoedsfall = adresseVedDoedsfall()

  return bostedadresseVedDoedsfall ? (
    <BodyShort>{formaterAdresse(bostedadresseVedDoedsfall)}</BodyShort>
  ) : (
    <BodyShort>Ingen adresse ved dødsfall</BodyShort>
  )
}
