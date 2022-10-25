import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { IBehandlingsType, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import classNames from 'classnames'
import { useAppSelector } from '../../../store/Store'
import { VilkaarsvurderingResultat } from '../../../shared/api/vilkaarsvurdering'

export const StegMeny = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const gyldighet =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const vilkaar =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.OPPFYLT

  const avdoedesBarn = behandling.familieforhold?.avdoede.opplysning.avdoedesBarn
  const soekerHarSoesken = avdoedesBarn ? avdoedesBarn.length > 1 : false

  return (
    <StegMenyWrapper>
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? (
        <li>
          <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
        </li>
      ) : null}
      <li className={classNames({ disabled: !gyldighet })}>
        <NavLink to="inngangsvilkaar">Vilkårsvurdering</NavLink>
      </li>
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && soekerHarSoesken ? (
        <li className={classNames({ disabled: !gyldighet || !vilkaar })}>
          <NavLink to="beregningsgrunnlag">Beregningsgrunnlag</NavLink>
        </li>
      ) : null}
      <li className={classNames({ disabled: !gyldighet || !vilkaar })}>
        <NavLink to="beregne">Beregning</NavLink>
      </li>
      <li>
        <NavLink to="brev">Brev</NavLink>
      </li>
    </StegMenyWrapper>
  )
}

const StegMenyWrapper = styled.ul`
  height: 100px;
  list-style: none;
  padding: 1em 0em 0;

  li {
    a {
      display: block;
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
        border-left: 8px solid #0067c5;
        color: #262626;
      }
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
