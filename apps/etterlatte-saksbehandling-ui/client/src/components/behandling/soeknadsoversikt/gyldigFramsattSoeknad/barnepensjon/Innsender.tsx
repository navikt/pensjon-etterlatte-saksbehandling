import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePdl } from '~shared/types/kilde'

interface Props {
  persongalleriGrunnlag: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevendeGrunnlag: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
}

export const Innsender = ({ persongalleriGrunnlag, gjenlevendeGrunnlag }: Props) => {
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const oppfylt = persongalleriGrunnlag.opplysning.innsender == gjenlevende?.foedselsnummer
  const navn = oppfylt ? [gjenlevende?.fornavn, gjenlevende?.mellomnavn, gjenlevende?.etternavn].join(' ') : 'Ukjent'
  const label = 'Innsender'
  const tekst = settTekst(oppfylt)

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

  return (
    <InfoWrapper>
      <Info tekst={navn ?? 'Ukjent'} undertekst={tekst} label={label} />
    </InfoWrapper>
  )
}
