import { Vilkaarsgrunnlag } from '../../../../shared/api/vilkaarsvurdering'
import { KildeType } from '../../../../store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { VilkaarColumn } from '../styled'
import { hentKildenavn } from '../utils'
import { formaterStringDato } from '../../../../utils/formattering'

export const AlderBarn = ({ grunnlag }: { grunnlag: Vilkaarsgrunnlag<any>[] }) => {
  const foedselsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'FOEDSELSDATO')
  const doedsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'DOEDSDATO')
  const virkningstidspunktGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'VIRKNINGSTIDSPUNKT')

  const foedselsdatoKilde = foedselsdatoGrunnlag?.kilde
  const foedselsdato = foedselsdatoGrunnlag?.opplysning.foedselsdato
  const doedsdato = doedsdatoGrunnlag?.opplysning.doedsdato
  const doedsdatoKilde = doedsdatoGrunnlag?.kilde
  const virkningstidspunkt = virkningstidspunktGrunnlag?.opplysning
  const virkningstidspunktKilde = virkningstidspunktGrunnlag?.kilde

  return (
    <>
      {doedsdato && doedsdatoKilde && (
        <VilkaarColumn>
          <Center>
            <div>
              <strong>Dødsfall</strong>
            </div>
            <span>{formaterStringDato(doedsdato)}</span>
            <KildeDatoOpplysning type={doedsdatoKilde.type} dato={doedsdatoKilde.tidspunktForInnhenting} />
          </Center>
        </VilkaarColumn>
      )}
      {foedselsdato && foedselsdatoKilde && (
        <VilkaarColumn>
          <Center>
            <div>
              <strong>Barnets fødselsdato</strong>
            </div>
            <span>{formaterStringDato(foedselsdato)}</span>
            <KildeDatoOpplysning type={foedselsdatoKilde.type} dato={foedselsdatoKilde.tidspunktForInnhenting} />
          </Center>
        </VilkaarColumn>
      )}
      {virkningstidspunkt && virkningstidspunktKilde && (
        <VilkaarColumn>
          <Center>
            <div>
              <strong>Virkningstidspunkt</strong>
            </div>
            <span>{formaterStringDato(virkningstidspunkt)}</span>
            <KildeDatoOpplysning type={virkningstidspunktKilde.type} dato={virkningstidspunktKilde.tidspunkt} />
          </Center>
        </VilkaarColumn>
      )}
    </>
  )
}

const KildeDatoOpplysning = ({ type, dato }: { type?: KildeType; dato?: string }) => {
  if (!dato) {
    return <div />
  }
  const dataDato = formaterStringDato(dato)
  const kilde = hentKildenavn(type)

  return (
    <KildeOppysning>
      {kilde} {dataDato}
    </KildeOppysning>
  )
}

export const KildeOppysning = styled.div`
  color: grey;
  font-size: 0.9em;
  margin-top: 5px;
`

export const Center = styled.div`
  text-align: center;
`
