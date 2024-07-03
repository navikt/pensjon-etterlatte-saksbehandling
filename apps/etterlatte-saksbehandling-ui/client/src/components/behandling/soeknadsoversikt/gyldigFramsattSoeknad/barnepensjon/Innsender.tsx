import { Info } from '../../Info'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

interface Props {
  harKildePesys: Boolean
}

export const Innsender = ({ harKildePesys }: Props) => {
  const personer = usePersonopplysninger()
  const label = 'Innsender'
  if (harKildePesys) {
    return <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
  }
  const gjenlevende = personer?.gjenlevende.find((person) => person)?.opplysning
  const innsender = personer?.innsender?.opplysning
  const soeker = personer?.soeker?.opplysning

  const navn = innsender ? (
    <>
      {formaterNavn(innsender) + ' '}
      <KopierbarVerdi value={innsender.foedselsnummer} />
    </>
  ) : (
    'Ukjent'
  )

  const undertekst = beskrivelseInnsender(soeker, gjenlevende, innsender)

  return <Info tekst={navn} undertekst={undertekst} label={label} />

  function beskrivelseInnsender(soeker?: IPdlPerson, gjenlevende?: IPdlPerson, innsender?: IPdlPerson): string {
    if (!innsender?.foedselsnummer) {
      return 'Mangler info'
    }
    if (innsender.foedselsnummer === gjenlevende?.foedselsnummer) {
      return '(gjenlevende forelder)'
    }
    if (innsender.foedselsnummer === soeker?.foedselsnummer) {
      return '(søker)'
    }
    return 'Ikke gjenlevende forelder'
  }
}
