import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import { Vilkaarsgrunnlag } from '../../../../shared/api/vilkaarsvurdering'
import { KildeType } from '../../../../store/reducers/BehandlingReducer'
import { hentKildenavn } from '../../inngangsvilkaar/vilkaar/tekstUtils'
import styled from 'styled-components'

export const AlderBarn = ({ grunnlag }: { grunnlag: Vilkaarsgrunnlag<any>[] }) => {
  const foedselsdatoGrunnlag = grunnlag.find((grunnlag) => grunnlag.opplysningsType == 'FOEDSELSDATO')

  const kilde = foedselsdatoGrunnlag?.kilde
  const foedselsdato = foedselsdatoGrunnlag?.opplysning.foedselsdato
  const barnetsAlder = !!foedselsdato ? differenceInYears(new Date(), new Date(foedselsdato)) : undefined

  if (!foedselsdatoGrunnlag || !kilde) return <div />

  return (
    <>
      <div>
        <strong>Barnets fødselsdato</strong>
      </div>
      <span>
        {format(new Date(foedselsdato), 'dd.MM.yyyy')}
        {barnetsAlder && <span> ({barnetsAlder} år)</span>}
      </span>
      <KildeDatoOpplysning type={kilde.type} dato={kilde.tidspunktForInnhenting} />
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
