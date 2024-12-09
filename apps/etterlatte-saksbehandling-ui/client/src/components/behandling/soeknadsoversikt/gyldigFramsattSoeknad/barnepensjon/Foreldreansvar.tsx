import { Info } from '../../Info'
import { Personopplysning } from '~shared/types/grunnlag'
import { IPdlPerson } from '~shared/types/Person'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

interface Props {
  gjenlevendeGrunnlag: Personopplysning | undefined
  soekerGrunnlag: Personopplysning | undefined
  harKildePesys: boolean
  innsender?: string | null
  avdoed?: string[]
}

export const Foreldreansvar = ({ gjenlevendeGrunnlag, soekerGrunnlag, harKildePesys, innsender, avdoed }: Props) => {
  const label = 'Foreldreansvar'
  if (harKildePesys) {
    return <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
  }

  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const ansvarligeForeldre = soekerGrunnlag?.opplysning?.familieRelasjon?.ansvarligeForeldre || []
  const oppfylt = innsender != undefined && ansvarligeForeldre.includes(innsender)
  const tekst = settTekst(oppfylt)

  const levendeMedAnsvar = ansvarligeForeldre.filter((it) => !avdoed?.includes(it))

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
