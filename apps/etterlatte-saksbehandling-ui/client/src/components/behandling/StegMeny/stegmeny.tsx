import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { IBehandlingsType, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import classNames from 'classnames'
import { Next } from '@navikt/ds-icons'
import { useAppSelector } from '../../../store/Store'

export const StegMeny = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const gyldighet =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const vilkaar =
    behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING ||
    behandling.vilkårsprøving?.resultat === VurderingsResultat.OPPFYLT

  const avdoedesBarn = behandling.familieforhold?.avdoede.opplysning.avdoedesBarn
  const soekerHarSoesken = avdoedesBarn ? avdoedesBarn.length > 1 : false

  return (
    <StegMenyWrapper>
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && (
        <>
          <li>
            <NavLink to="soeknadsoversikt">Søknadsoversikt</NavLink>
          </li>
          <Separator />
        </>
      )}
      <li className={classNames({ disabled: !gyldighet })}>
        <NavLink to="inngangsvilkaar">Vilkårsvurdering</NavLink>
      </li>
      <Separator />
      {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING && soekerHarSoesken && (
        <>
          <li className={classNames({ disabled: !gyldighet || !vilkaar })}>
            <NavLink to="beregningsgrunnlag">Beregningsgrunnlag</NavLink>
          </li>
          <Separator />
        </>
      )}
      <li className={classNames({ disabled: !gyldighet || !vilkaar })}>
        <NavLink to="beregne">Beregning</NavLink>
      </li>
      <Separator />
      <li>
        <NavLink to="brev">Brev</NavLink>
      </li>
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
