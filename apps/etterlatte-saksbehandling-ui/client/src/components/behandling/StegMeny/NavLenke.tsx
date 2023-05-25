import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import classNames from 'classnames'
import { behandlingSkalSendeBrev, kanGaaTilStatus } from '~components/behandling/felles/utils'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { PathInfo } from '~components/behandling/BehandlingRoutes'

export const NavLenke = (props: { pathInfo: PathInfo; behandling: IBehandlingReducer }) => {
  const { pathInfo } = props
  const { status } = props.behandling
  const stegErDisabled = (steg: IBehandlingStatus) => !kanGaaTilStatus(status).includes(steg)
  if (pathInfo.path == 'brev') {
    let status = IBehandlingStatus.BEREGNET //BP krav for å gå til brev
    if (props.behandling.sakType === ISaksType.OMSTILLINGSSTOENAD) {
      status = IBehandlingStatus.AVKORTET
    }
    return (
      <>
        {behandlingSkalSendeBrev(props.behandling) && (
          <>
            <li className={classNames({ disabled: stegErDisabled(status) })}>
              <NavLink to="brev">Vedtaksbrev</NavLink>
            </li>
          </>
        )}
      </>
    )
  }

  const routeErDisabled = pathInfo.disabledSteg && stegErDisabled(pathInfo.disabledSteg)
  return (
    <>
      <li className={classNames({ disabled: routeErDisabled })}>
        <NavLink to={pathInfo.path}>{pathInfo.description}</NavLink>
      </li>
      <Separator aria-hidden={'true'} />
    </>
  )
}

const Separator = styled(Next)`
  vertical-align: middle;
`
