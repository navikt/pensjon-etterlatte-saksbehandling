import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePdl } from '~shared/types/kilde'

interface Props {
  persongalleriGrunnlag: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevendeGrunnlag: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
  harKildePesys: Boolean
}

export const Innsender = ({ persongalleriGrunnlag, gjenlevendeGrunnlag, harKildePesys }: Props) => {
  const label = 'Innsender'
  if (harKildePesys) {
    return (
      <InfoWrapper>
        <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
      </InfoWrapper>
    )
  }
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const oppfylt = persongalleriGrunnlag.opplysning.innsender == gjenlevende?.foedselsnummer
  const navn = oppfylt ? [gjenlevende?.fornavn, gjenlevende?.mellomnavn, gjenlevende?.etternavn].join(' ') : 'Ukjent'
  const tekst = settTekst(oppfylt)

  return (
    <InfoWrapper>
      <Info tekst={navn ?? 'Ukjent'} undertekst={tekst} label={label} />
    </InfoWrapper>
  )

  function settTekst(vurdering: Boolean): string {
    switch (vurdering) {
      case true:
        return '(gjenlevende forelder)'
      case false:
        return 'Ikke gjenlevende forelder'
      default:
        return 'Mangler info'
    }
  }
}
