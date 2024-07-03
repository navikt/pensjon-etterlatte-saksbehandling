import { Info } from '../../Info'
import { Grunnlagsopplysning, Personopplysning } from '~shared/types/grunnlag'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { KildePdl } from '~shared/types/kilde'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

interface Props {
  persongalleriGrunnlag: Grunnlagsopplysning<Persongalleri, KildePdl>
  gjenlevendeGrunnlag: Personopplysning | undefined
  soekerGrunnlag: Personopplysning | undefined
  harKildePesys: Boolean
}

export const Foreldreansvar = ({
  persongalleriGrunnlag,
  gjenlevendeGrunnlag,
  soekerGrunnlag,
  harKildePesys,
}: Props) => {
  const label = 'Foreldreansvar'
  if (harKildePesys) {
    return <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
  }

  const persongalleri = persongalleriGrunnlag.opplysning
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const ansvarligeForeldre = soekerGrunnlag?.opplysning?.familieRelasjon?.ansvarligeForeldre || []
  const oppfylt = persongalleri.innsender != undefined && ansvarligeForeldre.includes(persongalleri.innsender)
  const tekst = settTekst(oppfylt)

  const levendeMedAnsvar = ansvarligeForeldre.filter((it) => !persongalleri.avdoed?.includes(it))

  const foreldreansvar = (
    <>
      {levendeMedAnsvar.map((fnr) => (
        <span key={`ansvar-${fnr}`}>
          {gjenlevende?.foedselsnummer == fnr ? fulltNavn(gjenlevende) + ' ' : 'Ukjent '}
          <KopierbarVerdi value={fnr} />
        </span>
      ))}
    </>
  )

  function settTekst(oppfylt: boolean): string {
    switch (oppfylt) {
      case true:
        return ''
      case false:
        return 'Innsender har ikke foreldreansvar'
    }
  }

  function fulltNavn(person: IPdlPerson | undefined) {
    return person ? [person?.fornavn, person?.mellomnavn, person?.etternavn].join(' ') : 'Ukjent'
  }

  return <Info tekst={foreldreansvar ?? 'Ukjent'} undertekst={tekst} label={label} />
}
