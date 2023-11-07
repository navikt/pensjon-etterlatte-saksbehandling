import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { KildePdl } from '~shared/types/kilde'

interface Props {
  persongalleri: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevende: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
}

export const Foreldreansvar = ({ persongalleri, gjenlevende }: Props) => {
  const oppfylt = persongalleri.opplysning.innsender == gjenlevende?.opplysning.foedselsnummer
  const navn = oppfylt
    ? [gjenlevende?.opplysning.fornavn, gjenlevende?.opplysning.mellomnavn, gjenlevende?.opplysning.etternavn].join(' ')
    : 'Ukjent'
  const label = 'Foreldreansvar'
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
