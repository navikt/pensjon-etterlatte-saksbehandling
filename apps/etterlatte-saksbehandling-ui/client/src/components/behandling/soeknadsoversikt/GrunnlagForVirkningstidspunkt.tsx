import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
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
          tekst={avdod?.opplysning.doedsdato ? formaterStringDato(avdod?.opplysning.doedsdato) : 'Ikke registrert'}
          undertekst={formaterGrunnlagKilde(avdod?.kilde)}
        />
      ))}
      {avdoede.length == 0 && <Info key="manglerDoedsdato" label="Dødsdato" tekst="Ikke registrert" />}
      {behandling?.soeknadMottattDato && (
        <Info key="soeknadMottatt" label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />
      )}
    </>
  )
}
