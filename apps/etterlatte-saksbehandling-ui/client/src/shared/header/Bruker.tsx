import { useContext } from 'react'
import styled from 'styled-components'
import { AppContext, IAppContext } from '../../store/AppContext'

export const Bruker = () => {
  const user = useContext<IAppContext>(AppContext).state.saksbehandlerReducer

  return (
    <BrukerWrap>
      {user?.fornavn} {user?.etternavn} <Ident>({user?.ident})</Ident>
    </BrukerWrap>
  )
}

const BrukerWrap = styled.div`
  padding: 1em;
`

const Ident = styled.span`
  font-size: 0.8em;
`
