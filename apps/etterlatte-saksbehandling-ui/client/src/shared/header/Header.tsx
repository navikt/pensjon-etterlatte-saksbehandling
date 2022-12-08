import styled from 'styled-components'
import { Bruker } from './Bruker'
import { Search } from './Search'
import { Link } from '@navikt/ds-react'

export const Header = () => {
  return (
    <>
      <HeaderWrapper>
        <Title href={'/'}>Doffen</Title>
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

const Title = styled(Link)`
  font-size: 1.2em;
  text-decoration: none;
  color: #fff;

  &:hover,
  &:focus {
    color: #0067c5;
    cursor: pointer;
    background: none;
  }
`
const RightWrap = styled.div`
  display: flex;
  align-items: center;
  padding: 0;
`
