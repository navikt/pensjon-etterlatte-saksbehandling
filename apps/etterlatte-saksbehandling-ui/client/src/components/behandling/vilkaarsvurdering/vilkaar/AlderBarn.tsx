import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import { Vilkaarsgrunnlag } from '../../../../shared/api/vilkaarsvurdering'
import { KildeType } from '../../../../store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { VilkaarColumn } from '../styled'
import { hentKildenavn } from '../utils'

export const AlderBarn = ({ grunnlag }: { grunnlag: Vilkaarsgrunnlag<any>[] }) => {
  const foedselsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'FOEDSELSDATO')
  const doedsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'DOEDSDATO')

  const foedselsdatoKilde = foedselsdatoGrunnlag?.kilde
  const foedselsdato = foedselsdatoGrunnlag?.opplysning.foedselsdato
  const barnetsAlder = !!foedselsdato ? differenceInYears(new Date(), new Date(foedselsdato)) : undefined
  const doedsdato = doedsdatoGrunnlag?.opplysning.doedsdato
  const doedsdatoKilde = doedsdatoGrunnlag?.kilde
  const barnetsAlderVedDoedsfall = differenceInYears(new Date(doedsdato), new Date(foedselsdato))

  return (
    <>
      {foedselsdato && foedselsdatoKilde && (
        <VilkaarColumn>
          <Center>
            <div>
              <strong>Barnets fødselsdato</strong>
            </div>
            <span>
              {format(new Date(foedselsdato), 'dd.MM.yyyy')}
              {barnetsAlder && <span> ({barnetsAlder} år)</span>}
            </span>
            <KildeDatoOpplysning type={foedselsdatoKilde.type} dato={foedselsdatoKilde.tidspunktForInnhenting} />
          </Center>
        </VilkaarColumn>
      )}
      {doedsdato && doedsdatoKilde && (
        <VilkaarColumn>
          <Center>
            <div>
              <strong>Alder ved dødsfall</strong>
            </div>
            <span>
              {barnetsAlderVedDoedsfall} år ({format(new Date(doedsdato), 'dd.MM.yyyy')})
            </span>
            <KildeDatoOpplysning type={doedsdatoKilde.type} dato={doedsdatoKilde.tidspunktForInnhenting} />
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
  const dataDato = format(new Date(dato), 'dd.MM.yyyy')
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
