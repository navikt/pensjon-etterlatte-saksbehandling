import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { IBehandlingsType, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import classNames from 'classnames'

export const StegMeny = () => {
  const ctx = useContext(AppContext)
  const {
    state: { behandlingReducer },
  } = ctx

  const gyldighet =
    behandlingReducer.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandlingReducer.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const vilkaar =
    behandlingReducer.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandlingReducer.vilkårsprøving?.resultat === VurderingsResultat.OPPFYLT

  const avdoedesBarn = behandlingReducer.familieforhold?.avdoede.opplysning.avdoedesBarn
  const soekerHarSoesken = avdoedesBarn ? avdoedesBarn.length > 1 : false

  return (
    <StegMenyWrapper>
      {behandlingReducer.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? (
        <li>
          <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
        </li>
      ) : null}
      <li className={classNames({ disabled: !gyldighet })}>
        <NavLink to="inngangsvilkaar">Vilkårsvurdering</NavLink>
      </li>
      {behandlingReducer.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && soekerHarSoesken ? (
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
