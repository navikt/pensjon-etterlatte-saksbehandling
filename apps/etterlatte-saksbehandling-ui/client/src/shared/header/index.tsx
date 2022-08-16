import { useState } from 'react'
import styled from 'styled-components'
import { SystemIcon } from '../icons/systemIcon'
import { Bruker } from './Bruker'
import { Search } from './Search'

export const Header = () => {
  const [menuOpen, setMenuOpen] = useState(false)

  const toggleMenu = () => {
    setMenuOpen(!menuOpen)
  }

  return (
    <>
      <HeaderWrapper>
        <Title>Etterlatte - saksbehandling</Title>
        <RightWrap>
          <Search />
          <Menu onClick={toggleMenu}>
            <SystemIcon />
          </Menu>
          <Bruker />
        </RightWrap>
      </HeaderWrapper>

      {menuOpen && <MenuContent>Her kommer det innhold kanskje</MenuContent>}
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
`
const RightWrap = styled.div`
  display: flex;
  align-items: center;
  padding: 0;
`

const Menu = styled.div`
  border-right: 1px solid #888;
  height: 100%;
  padding: 0 1em;
  cursor: pointer;
`

const MenuContent = styled.div`
  position: absolute;
  right: 0;
  background-color: #262626;
  color: #fff;
  width: 400px;
  height: 400px;
  padding: 1em;
`
