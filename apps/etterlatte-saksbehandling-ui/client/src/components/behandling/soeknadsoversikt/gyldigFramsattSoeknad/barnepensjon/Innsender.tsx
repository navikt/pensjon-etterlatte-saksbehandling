import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { Personopplysning } from '~shared/types/grunnlag'
import { Link } from '@navikt/ds-react'
import { formaterFnr } from '~utils/formattering'

interface Props {
  gjenlevendeGrunnlag: Personopplysning | undefined
  innsender: Personopplysning | undefined
  harKildePesys: Boolean
}

export const Innsender = ({ gjenlevendeGrunnlag, innsender, harKildePesys }: Props) => {
  const label = 'Innsender'
  if (harKildePesys) {
    return (
      <InfoWrapper>
        <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
      </InfoWrapper>
    )
  }
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const innsenderErGjenlevende = innsender?.opplysning.foedselsnummer == gjenlevende?.foedselsnummer

  const navn = innsender ? (
    <>
      {fulltNavn(innsender) + ' '}
      <Link href={`/person/${innsender.opplysning.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
        ({formaterFnr(innsender.opplysning.foedselsnummer)})
      </Link>
    </>
  ) : (
    'Ukjent'
  )

  const tekst = innsenderErGjenlevende ? '(gjenlevende forelder)' : 'Ikke gjenlevende forelder'

  return (
    <InfoWrapper>
      <Info tekst={navn} undertekst={tekst} label={label} />
    </InfoWrapper>
  )

  function fulltNavn(innsender: Personopplysning) {
    return [innsender.opplysning.fornavn, innsender.opplysning.mellomnavn, innsender.opplysning.etternavn].join(' ')
  }
}
