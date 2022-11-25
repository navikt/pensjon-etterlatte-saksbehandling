import styled from 'styled-components'
import { Bruker } from './Bruker'
import { Search } from './Search'
import { useNavigate } from 'react-router-dom'

export const Header = () => {
  const navigate = useNavigate()

  const goToOppgavebenken = () => {
    navigate('/')
  }

  return (
    <>
      <HeaderWrapper>
        <Title onClick={goToOppgavebenken}>Etterlatte - saksbehandling</Title>
        <RightWrap>
          <Search />
          <Bruker />
        </RightWrap>
      </HeaderWrapper>
    </>
  )
}

const HeaderWrapper = styled.div`
  background-color: #262626;
  color: #fff;
  display: flex;
  justify-content: space-between;
  padding: 0 1em;
  height: 60px;
  align-items: center;
`

const Title = styled.div`
  font-size: 1.2em;
  &:hover,
  &:focus {
    color: #0067c5;
    cursor: pointer;
  }
`
const RightWrap = styled.div`
  display: flex;
  align-items: center;
  padding: 0;
`
