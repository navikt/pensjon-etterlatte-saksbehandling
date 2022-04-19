import { format } from 'date-fns'
import { hentKildenavn } from './utils'
import styled from 'styled-components'

export const KildeDatoOpplysning = ({ type, dato }: { type: String; dato: string }) => {
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

export const KildeDatoVilkaar = ({ type, dato }: { type: String; dato: string }) => {
  const dataDato = format(new Date(dato), 'dd.MM.yyyy')
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
  padding-left: 55px;
  margin-bottom: 30px;
`
