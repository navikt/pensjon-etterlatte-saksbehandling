import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { Grunnlagsopplysning, Personopplysning } from '~shared/types/grunnlag'
import { Persongalleri } from '~shared/types/Person'
import { KildePdl } from '~shared/types/kilde'

interface Props {
  persongalleriGrunnlag: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevendeGrunnlag: Personopplysning | undefined
  harKildePesys: Boolean
}

export const Foreldreansvar = ({ persongalleriGrunnlag, gjenlevendeGrunnlag, harKildePesys }: Props) => {
  const label = 'Foreldreansvar'
  if (harKildePesys) {
    return (
      <InfoWrapper>
        <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
      </InfoWrapper>
    )
  }

  const persongalleri = persongalleriGrunnlag.opplysning
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const oppfylt = persongalleri.innsender == gjenlevende?.foedselsnummer
  const navn = oppfylt ? [gjenlevende?.fornavn, gjenlevende?.mellomnavn, gjenlevende?.etternavn].join(' ') : 'Ukjent'
  const tekst = settTekst(oppfylt)

  function settTekst(oppfylt: Boolean): string {
    switch (oppfylt) {
      case true:
        return ''
      case false:
        return 'Innsender har ikke foreldreansvar'
      default:
        return 'Mangler info'
    }
  }

  return (
    <InfoWrapper>
      <Info tekst={navn ?? 'Ukjent'} undertekst={tekst} label={label} />
    </InfoWrapper>
  )
}
