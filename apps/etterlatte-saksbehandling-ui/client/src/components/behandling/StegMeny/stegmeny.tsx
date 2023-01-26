import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import classNames from 'classnames'
import { Next } from '@navikt/ds-icons'
import { useAppSelector } from '~store/Store'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { behandlingErUtfylt } from '~components/behandling/felles/utils'
import { JaNei } from '~shared/types/ISvar'

export const StegMeny = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const klarForVidereBehandling = behandlingErUtfylt(behandling) && behandling.kommerBarnetTilgode?.svar === JaNei.JA
  const gyldighet =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const vilkaar =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    (behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.OPPFYLT && klarForVidereBehandling)

  const harBeregning = !!behandling.beregning

  const oppfylt = behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.OPPFYLT
  const vurdert = !!behandling.vilkårsprøving?.resultat && klarForVidereBehandling

  return (
    <StegMenyWrapper role="navigation">
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && (
        <>
          <li>
            <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandling.behandlingType === IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li>
            <NavLink to="opphoeroversikt">Opphør</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <li className={classNames({ disabled: !klarForVidereBehandling || !gyldighet })}>
            <NavLink to="vilkaarsvurdering">Vilkårsvurdering</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && (
        <>
          <li className={classNames({ disabled: !gyldighet || !vilkaar })}>
            <NavLink to="beregningsgrunnlag">Beregningsgrunnlag</NavLink>
          </li>
          <Separator aria-hidden={'true'} />
        </>
      )}
      <li className={classNames({ disabled: !gyldighet || !vilkaar || !harBeregning })}>
        <NavLink to="beregne">Beregning</NavLink>
      </li>
      {behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER && (
        <>
          <Separator aria-hidden={'true'} />
          <li
            className={classNames({
              disabled: !vurdert || (oppfylt && !harBeregning),
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
