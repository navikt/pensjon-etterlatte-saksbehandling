import { Personopplysning } from '~shared/types/grunnlag'
import { add, isAfter, isBefore, sub } from 'date-fns'
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

      // Siden vi bruker isAfter / isBefore må vi justere dødsdato slik at den er med i sammenligningen
      const enDagFoerDoedsdato = sub(avdoed.opplysning.doedsdato!, { days: 1 })
      const enDagEtterDoedsdato = add(avdoed.opplysning.doedsdato!, { days: 1 })
      return kopiAvAdoedesBostedsadresser?.find(
        (adresse) =>
          isAfter(enDagEtterDoedsdato, adresse.gyldigFraOgMed ?? new Date()) &&
          isBefore(enDagFoerDoedsdato, adresse.gyldigTilOgMed ?? new Date())
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
