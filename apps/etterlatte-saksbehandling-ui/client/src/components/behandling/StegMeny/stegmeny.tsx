import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'

export const StegMeny = () => {
  const ctx = useContext(AppContext)
  const gyldighet = ctx.state.behandlingReducer.gyldighetsprøving.resultat === VurderingsResultat.OPPFYLT
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving.resultat === VurderingsResultat.OPPFYLT

  return (
    <StegMenyWrapper>
      <li>
        <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
      </li>
      <li className={!gyldighet ? 'disabled' : ''}>
        <NavLink to="inngangsvilkaar">Vilkårsvurdering</NavLink>
      </li>
      <li className={!gyldighet || !vilkaar ? 'disabled' : ''}>
        <NavLink to="beregne">Beregning</NavLink>
      </li>
      {/*
      <li className={!gyldighet || !vilkaar ? 'disabled' : ''}>
        <NavLink to="utbetalingsoversikt">Simulering til oppdrag</NavLink>
      </li>
      <li className={!gyldighet || !vilkaar ? 'disabled' : ''}>
        <NavLink to="brev">Brev</NavLink>
      </li>
      */}
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
