import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import classNames from 'classnames'
import { kanGaaTilStatus } from '~components/behandling/felles/utils'

export const NavLenke = (props: { path: string; status: IBehandlingStatus }) => {
  if (props.path == 'brev') {
    return <></>
  }
  const stegErDisabled = (steg: IBehandlingStatus) => !kanGaaTilStatus(props.status).includes(steg)

  return (
    <>
      <li
        className={classNames({
          disabled:
            props.path.toUpperCase() in IBehandlingStatus ? stegErDisabled(IBehandlingStatus.VILKAARSVURDERT) : false,
        })}
      >
        <NavLink to={props.path}>{makeFirstLetterCapitalized(props.path)}</NavLink>
      </li>
      <Separator aria-hidden={'true'} />
    </>
  )
}

const Separator = styled(Next)`
  vertical-align: middle;
`

const makeFirstLetterCapitalized = (str: string) => str.charAt(0).toUpperCase() + str.slice(1)
