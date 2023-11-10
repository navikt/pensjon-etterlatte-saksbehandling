import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import classNames from 'classnames'
import { hentGyldigeNavigeringsStatuser } from '~components/behandling/felles/utils'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteTypes } from '~components/behandling/BehandlingRoutes'

export const NavLenke = (props: {
  pathInfo: BehandlingRouteTypes
  behandling: IBehandlingReducer
  separator: boolean
}) => {
  const { pathInfo, separator } = props
  const { status } = props.behandling
  const routeErDisabled =
    pathInfo.kreverBehandlingsstatus &&
    !hentGyldigeNavigeringsStatuser(status).includes(pathInfo.kreverBehandlingsstatus)

  return (
    <>
      <li className={classNames({ disabled: routeErDisabled })}>
        <NavLink to={pathInfo.path}>{pathInfo.description}</NavLink>
      </li>
      {separator && <Separator aria-hidden="true" />}
    </>
  )
}

export const Separator = styled(ChevronRightIcon)`
  vertical-align: middle;
`
