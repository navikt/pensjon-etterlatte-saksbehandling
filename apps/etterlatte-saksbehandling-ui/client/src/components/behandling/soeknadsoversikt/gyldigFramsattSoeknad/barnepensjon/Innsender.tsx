import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePdl } from '~shared/types/kilde'

interface Props {
  persongalleri: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevende: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
}

export const Innsender = ({ persongalleri, gjenlevende }: Props) => {
  const oppfylt = persongalleri.opplysning.innsender == gjenlevende?.opplysning.foedselsnummer
  const navn = oppfylt
    ? [gjenlevende?.opplysning.fornavn, gjenlevende?.opplysning.mellomnavn, gjenlevende?.opplysning.etternavn].join(' ')
    : 'Ukjent'
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
