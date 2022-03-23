import moment from 'moment'
import { hentKildenavn } from './utils'
import styled from 'styled-components'

export const KildeDato = ({ type, dato }: { type: String; dato: Date }) => {
  const dataDato = moment(dato).format('DD.MM.YYYY')
  const kilde = hentKildenavn(type)

  return (
    <Kilde>
      {kilde} {dataDato}
    </Kilde>
  )
}

export const Kilde = styled.div`
  color: grey;
  font-size: 0.9em;
  margin-top: 5px;
`
