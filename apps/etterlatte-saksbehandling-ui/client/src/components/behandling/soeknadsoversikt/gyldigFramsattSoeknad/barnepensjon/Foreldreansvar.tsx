import { InfoWrapper } from '../../styled'
import { Info } from '../../Info'
import { Grunnlagsopplysning, Personopplysning } from '~shared/types/grunnlag'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { KildePdl } from '~shared/types/kilde'
import { formaterFnr } from '~utils/formattering'
import { Link } from '@navikt/ds-react'

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
    return (
      <InfoWrapper>
        <Info tekst="Ukjent" undertekst="Mangler info" label={label} />
      </InfoWrapper>
    )
  }

  const persongalleri = persongalleriGrunnlag.opplysning
  const gjenlevende = gjenlevendeGrunnlag?.opplysning
  const ansvarligeForeldre = soekerGrunnlag?.opplysning?.familieRelasjon?.ansvarligeForeldre || []
  const oppfylt = persongalleri.innsender != undefined && ansvarligeForeldre.includes(persongalleri.innsender)
  const tekst = settTekst(oppfylt)

  console.log('inns ' + persongalleri.innsender)
  console.log('ansv ' + ansvarligeForeldre)
  const levendeMedAnsvar = ansvarligeForeldre.filter((it) => !persongalleri.avdoed?.includes(it))

  const foreldreansvar = (
    <>
      {levendeMedAnsvar.map((fnr) => (
        <>
          {gjenlevende?.foedselsnummer == fnr ? fulltNavn(gjenlevende) + ' ' : 'Ukjent '}
          <Link href={`/person/${fnr}`} target="_blank" rel="noreferrer noopener">
            ({formaterFnr(fnr)})
          </Link>
        </>
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

  return (
    <InfoWrapper>
      <Info tekst={foreldreansvar ?? 'Ukjent'} undertekst={tekst} label={label} />
    </InfoWrapper>
  )
}
