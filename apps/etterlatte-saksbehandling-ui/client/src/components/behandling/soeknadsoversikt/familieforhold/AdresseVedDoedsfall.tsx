import { Personopplysning } from '~shared/types/grunnlag'
import { isAfter, isBefore } from 'date-fns'
import { BodyShort } from '@navikt/ds-react'
import { formaterAdresse } from '~shared/types/Person'
import { IAdresse } from '~shared/types/IAdresse'

export const AdresseVedDoedsfall = ({ avdoed }: { avdoed: Personopplysning }) => {
  const adresseVedDoedsfall = (): IAdresse | undefined => {
    if (!!avdoed.opplysning.doedsdato) {
      const kopiAvAdoedesBostedsadresser = [...(avdoed.opplysning.bostedsadresse ?? [])]
      // Sorterer bostedsadresser slik at nyligste fra-og-med dato kommer først
      kopiAvAdoedesBostedsadresser.sort((a, b) => {
        if (b.gyldigFraOgMed === undefined) {
          return -1
        }
        if (a.gyldigFraOgMed === undefined) {
          return 1
        }

        return new Date(b.gyldigFraOgMed).getUTCMilliseconds() - new Date(a.gyldigFraOgMed).getUTCMilliseconds()
      })

      return kopiAvAdoedesBostedsadresser?.find(
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
