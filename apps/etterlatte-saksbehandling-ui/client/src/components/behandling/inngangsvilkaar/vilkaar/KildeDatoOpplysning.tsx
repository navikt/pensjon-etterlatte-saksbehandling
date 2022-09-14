import { format } from 'date-fns'
import { hentKildenavn } from './tekstUtils'
import styled from 'styled-components'
import { formaterStringDato } from '../../../../utils/formattering'
import { KildeType } from '../../../../store/reducers/BehandlingReducer'

export const KildeDatoOpplysning = ({ type, dato }: { type?: KildeType; dato?: string }) => {
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

export const KildeDatoVilkaar = ({ isHelautomatisk, dato }: { isHelautomatisk: boolean; dato: string }) => {
  const dataDato = formaterStringDato(dato)
  const kilde = isHelautomatisk ? 'Automatisk' : 'Delautomatisk'

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
