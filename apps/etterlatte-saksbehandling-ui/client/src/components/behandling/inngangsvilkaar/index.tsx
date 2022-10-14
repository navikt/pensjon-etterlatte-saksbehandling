import { Content, Header } from '../../../shared/styled'
import { useEffect } from 'react'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'
import { IBehandlingsType, VilkaarsType } from '../../../store/reducers/BehandlingReducer'
import { AvdoedesForutMedlemskap } from './vilkaar/avdoedes_medlemskap/AvdoedesForutMedlemskap'
import { useLocation } from 'react-router-dom'
import { BarnetsMedlemskap } from './vilkaar/BarnetsMedlemskap'
import { VilkaarResultat } from './vilkaar/VilkaarResultat'
import { Virkningstidspunkt } from './vilkaar/Virkningstidspunkt'
import { VilkaarBorderTop } from './styled'
import { hentBehandlesFraStatus } from '../felles/utils'
import { Formaal } from './vilkaar/Formaal'
import styled from 'styled-components'
import { KanYtelsenBehandles } from './vilkaar/KanYtelsenBehandles'
import { useAppSelector } from '../../../store/Store'

const TekstMedMaksbredde = styled.p`
  max-width: 400px;
`

export const Inngangsvilkaar = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const location = useLocation()

  const behandlingstype = behandling.behandlingType
  const erFoerstegangsbehandling = behandlingstype === IBehandlingsType.FØRSTEGANGSBEHANDLING
  const erManueltOpphoer = behandlingstype === IBehandlingsType.MANUELT_OPPHOER

  const erRevurdering = behandlingstype === IBehandlingsType.REVURDERING
  const virkningstidspunkt = behandling.virkningstidspunkt
  const vilkaarsproving = behandling.vilkårsprøving
  const behandles = hentBehandlesFraStatus(behandling?.status)

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  if (vilkaarsproving == null || vilkaarsproving?.vilkaar?.length === 0) {
    return <div>Mangler vilkår</div>
  }

  const vilkaar = vilkaarsproving.vilkaar

  return (
    <Content>
      {erManueltOpphoer ? (
        <Header>
          <h1>Manuelt opphør</h1>
          <TekstMedMaksbredde>
            Det har kommet inn nye endringer på saken som gjør at den ikke kan behandles her. Saken må opprettes manuelt
            i Pesys, og deretter opphøres i nytt system. Se rutiner for å opprette sak i Pesys.
          </TekstMedMaksbredde>
        </Header>
      ) : (
        <>
          <Header>
            <h1>Vilkårsvurdering</h1>
          </Header>
          <VilkaarBorderTop />
        </>
      )}
      {erFoerstegangsbehandling ? (
        <>
          <Formaal
            id="formaal"
            vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.FORMAAL_FOR_YTELSEN)}
          />
          <AlderBarn
            id="alderbarn"
            vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)}
          />
          <DoedsFallForelder
            id="dodsfallforelder"
            vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)}
          />
          <AvdoedesForutMedlemskap
            id="avdodesmedlemskap"
            vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP)}
          />
          <BarnetsMedlemskap
            id="barnetsmedlemskap"
            vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.BARNETS_MEDLEMSKAP)}
          />
        </>
      ) : null}
      {erRevurdering ? (
        <Formaal id="formaal" vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.FORMAAL_FOR_YTELSEN)} />
      ) : null}
      {erManueltOpphoer ? (
        <KanYtelsenBehandles
          id="kanbehandles"
          vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SAKEN_KAN_BEHANDLES_I_SYSTEMET)}
        />
      ) : (
        <Virkningstidspunkt
          behandlingType={behandling.behandlingType}
          id="virkningstidspunkt"
          vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)}
          virkningsdato={virkningstidspunkt}
          mottattdato={behandling.soeknadMottattDato}
        />
      )}
      <VilkaarResultat id="vilkaarResultat" dato={virkningstidspunkt} behandles={behandles} />
    </Content>
  )
}
