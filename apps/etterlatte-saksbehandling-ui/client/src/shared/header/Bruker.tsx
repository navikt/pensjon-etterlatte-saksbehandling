import styled from 'styled-components'
import { useAppSelector } from '~store/Store'

export const Bruker = () => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  return (
    <BrukerWrap>
      {user.fornavn} {user.etternavn} <Ident>({user.ident})</Ident>
    </BrukerWrap>
  )
}

const BrukerWrap = styled.div`
  padding: 1em;
`

const Ident = styled.span`
  font-size: 0.8em;
`
