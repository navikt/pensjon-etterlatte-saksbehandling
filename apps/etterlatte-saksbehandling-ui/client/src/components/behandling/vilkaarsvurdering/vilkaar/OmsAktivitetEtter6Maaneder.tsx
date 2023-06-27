import { Vilkaarsgrunnlag } from '~shared/api/vilkaarsvurdering'
import { VilkaarColumn } from '../styled'
import { formaterStringDato } from '~utils/formattering'
import { VilkaarStrong } from './AlderBarn'

export const OmsAktivitetEtter6Maaneder = ({ grunnlag }: { grunnlag: Vilkaarsgrunnlag<any>[] }) => {
  const doedsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'AVDOED_DOEDSDATO')
  const soeknadMottattGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'SOEKNAD_MOTTATT_DATO')

  const doedsdato = doedsdatoGrunnlag?.opplysning
  const soeknadMottattDato = soeknadMottattGrunnlag?.opplysning

  return (
    <>
      {doedsdato && (
        <VilkaarColumn>
          <div>
            <VilkaarStrong>Dødsdato</VilkaarStrong>
          </div>
          <span>{formaterStringDato(doedsdato)}</span>
        </VilkaarColumn>
      )}
      {soeknadMottattDato && (
        <VilkaarColumn>
          <div>
            <VilkaarStrong>Søknad mottatt</VilkaarStrong>
          </div>
          <span>{formaterStringDato(soeknadMottattDato.mottattDato)}</span>
        </VilkaarColumn>
      )}
    </>
  )
}
