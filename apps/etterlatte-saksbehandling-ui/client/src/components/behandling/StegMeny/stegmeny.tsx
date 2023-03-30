import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import classNames from 'classnames'
import { Next } from '@navikt/ds-icons'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { kanGaaTilStatus } from '~components/behandling/felles/utils'
import { ISaksType } from '~components/behandling/fargetags/saksType'

export const StegMeny = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingType, sakType, status } = props.behandling
  const stegErDisabled = (steg: IBehandlingStatus) => !kanGaaTilStatus(status).includes(steg)

  return (
    <StegMenyWrapper role="navigation">
      {behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li>
            <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandlingType === IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li>
            <NavLink to="opphoeroversikt">Opphør</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li className={classNames({ disabled: stegErDisabled(IBehandlingStatus.VILKAARSVURDERT) })}>
            <NavLink to="vilkaarsvurdering">Vilkårsvurdering</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandlingType !== IBehandlingsType.MANUELT_OPPHOER && sakType === ISaksType.OMSTILLINGSSTOENAD && (
        <>
          <li className={classNames({ disabled: stegErDisabled(IBehandlingStatus.OPPRETTET) })}>
            <NavLink to="trygdetid">Trygdetid</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li className={classNames({ disabled: stegErDisabled(IBehandlingStatus.BEREGNET) })}>
            <NavLink to="beregningsgrunnlag">Beregningsgrunnlag</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      <li className={classNames({ disabled: stegErDisabled(IBehandlingStatus.BEREGNET) })}>
        <NavLink to="beregne">Beregning</NavLink>
      </li>
      {behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <Separator aria-hidden={'true'} />
          <li
            className={classNames({
              disabled: stegErDisabled(IBehandlingStatus.FATTET_VEDTAK),
            })}
          >
            <NavLink to="brev">Vedtaksbrev</NavLink>
          </li>
        </>
      )}
    </StegMenyWrapper>
  )
}

const StegMenyWrapper = styled.ul`
  display: block;
  list-style: none;
  padding: 1em 0;
  background: #f8f8f8;
  border-bottom: 1px solid #c6c2bf;
  box-shadow: 0 5px 10px 0 #ddd;

  li {
    display: inline-block;

    a {
      padding: 1em 1em 1em;
      margin-bottom: 0.4em;
      font-weight: 600;
      color: #0067c5;
      text-decoration: none;
      border-left: 8px solid transparent;

      &:hover {
        text-decoration: underline;
      }

      &.active {
        color: #262626;
      }
    }

    &:first-child a {
      border-left: 8px solid #0067c5;
    }
  }

  .disabled {
    cursor: not-allowed;

    a {
      color: #b0b0b0;
      text-decoration: none;
      pointer-events: none;
    }
  }
`

const Separator = styled(Next)`
  vertical-align: middle;
`
