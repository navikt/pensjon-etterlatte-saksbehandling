import { Content, Header } from '../../../shared/styled'
import { useContext, useEffect } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'
import { VilkaarsType } from '../../../store/reducers/BehandlingReducer'
import { AvdoedesForutMedlemskap } from './vilkaar/AvdoedesForutMedlemskap'
import { useLocation } from 'react-router-dom'
import { BarnetsMedlemskap } from './vilkaar/BarnetsMedlemskap'
import { VilkaarResultat } from './vilkaar/VilkaarResultat'
import { Virkningstidspunkt } from './vilkaar/Virkningstidspunkt'
import { VilkaarBorderTop } from './styled'
import { hentBehandlesFraStatus } from '../felles/utils'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const location = useLocation()

  const virkningstidspunkt = ctx.state.behandlingReducer.virkningstidspunkt
  const vilkaarsproving = ctx.state.behandlingReducer.vilkårsprøving
  const behandles = hentBehandlesFraStatus(ctx.state.behandlingReducer?.status)

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
      <Header>
        <h1>Vilkårsvurdering</h1>
      </Header>
      <VilkaarBorderTop />
      <AlderBarn id="alderbarn" vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)} />
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
      <Virkningstidspunkt
        id="virkningstidspunkt"
        vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)}
        virkningsdato={virkningstidspunkt}
        mottattdato={ctx.state.behandlingReducer.soeknadMottattDato}
      />
      <VilkaarResultat
        id="vilkaarResultat"
        resultat={vilkaarsproving.resultat}
        dato={virkningstidspunkt}
        behandles={behandles}
      />
    </Content>
  )
}
