import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { ChevronLeftIcon } from '@navikt/aksel-icons'

export default function NavigerTilbakeMeny({ label, path }: { label: string; path: string }) {
  return (
    <MenyWrapper role="navigation">
      <Link to={path || '/'}>
        <Separator /> {label}
      </Link>
    </MenyWrapper>
  )
}

const Link = styled(NavLink)`
  margin-left: 1rem;
  font-weight: 600;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
`

const MenyWrapper = styled.ul`
  display: block;
  list-style: none;
  padding: 1em 0;
  background: #f8f8f8;
  border-bottom: 1px solid #c6c2bf;
  box-shadow: 0 5px 10px 0 #ddd;
`

const Separator = styled(ChevronLeftIcon)`
  vertical-align: middle;
`
