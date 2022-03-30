import moment from 'moment'
import { hentKildenavn } from './utils'
import styled from 'styled-components'

export const KildeDatoOpplysning = ({ type, dato }: { type: String; dato: Date }) => {
  const dataDato = moment(dato).format('DD.MM.YYYY')
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

export const KildeDatoVilkaar = ({ type, dato }: { type: String; dato: Date }) => {
  const dataDato = moment(dato).format('DD.MM.YYYY')
  const kilde = hentKildenavn(type)

  return (
    <KildeVilkaar>
      {kilde} {dataDato}
    </KildeVilkaar>
  )
}
export const KildeVilkaar = styled.div`
  color: grey;
  font-size: 0.9em;
  padding-left: 51px;
  margin-bottom: 30px;
`
