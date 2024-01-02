import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

interface Props {
  harKildePesys: Boolean
}

export const Innsender = ({ harKildePesys }: Props) => {
  const personer = usePersonopplysninger()
  const label = 'Innsender'
  if (harKildePesys) {
    return (
      <InfoWrapper>
        <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
      </InfoWrapper>
    )
  }
  const gjenlevende = personer?.gjenlevende.find((a) => a)?.opplysning
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

  return (
    <InfoWrapper>
      <Info tekst={navn} undertekst={undertekst} label={label} />
    </InfoWrapper>
  )

  function beskrivelseInnsender(soeker?: IPdlPerson, innsender?: IPdlPerson, gjenlevende?: IPdlPerson): string {
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
