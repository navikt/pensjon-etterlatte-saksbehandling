import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const avdoede = personopplysninger?.avdoede || []
  return (
    <>
      {avdoede.map((avdod, index) => (
        <Info
          key={index}
          label="Dødsdato"
          tekst={
            <>
              {avdod.opplysning.fornavn} {avdod.opplysning.etternavn}
              <br />
              {avdod.opplysning.doedsdato ? formaterDato(avdod?.opplysning.doedsdato) : 'Ikke registrert'}
            </>
          }
          undertekst={formaterGrunnlagKilde(avdod?.kilde)}
        />
      ))}
      {!avdoede.length && <Info key="manglerDoedsdato" label="Dødsdato" tekst="Ikke registrert" />}
      {behandling?.soeknadMottattDato && (
        <Info key="soeknadMottatt" label="Søknad mottatt" tekst={formaterDato(behandling.soeknadMottattDato)} />
      )}
    </>
  )
}
