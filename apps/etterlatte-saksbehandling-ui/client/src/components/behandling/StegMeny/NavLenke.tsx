import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import classNames from 'classnames'
import { behandlingSkalSendeBrev, kanGaaTilStatus } from '~components/behandling/felles/utils'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ISaksType } from '~components/behandling/fargetags/saksType'

export const NavLenke = (props: { path: string; behandling: IBehandlingReducer }) => {
  const { status } = props.behandling
  const stegErDisabled = (steg: IBehandlingStatus) => !kanGaaTilStatus(status).includes(steg)
  if (props.path == 'brev') {
    let status = IBehandlingStatus.BEREGNET //BP krav for å gå til brev
    if (props.behandling.sakType === ISaksType.OMSTILLINGSSTOENAD) {
      status = IBehandlingStatus.AVKORTET
    }
    return (
      <>
        {behandlingSkalSendeBrev(props.behandling) && (
          <>
            <li
              className={classNames({
                disabled: stegErDisabled(status),
              })}
            >
              <NavLink to="brev">Vedtaksbrev</NavLink>
            </li>
          </>
        )}
      </>
    )
  }
  let routeIsDisabled = false
  if (props.path === 'vilkaarsvurdering') {
    routeIsDisabled = stegErDisabled(IBehandlingStatus.VILKAARSVURDERT)
  } else if (props.path === 'beregne') {
    routeIsDisabled = stegErDisabled(IBehandlingStatus.BEREGNET)
  } else if (props.path === 'beregningsgrunnlag') {
    routeIsDisabled = stegErDisabled(IBehandlingStatus.VILKAARSVURDERT)
  }
  return (
    <>
      <li
        className={classNames({
          disabled: routeIsDisabled,
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
