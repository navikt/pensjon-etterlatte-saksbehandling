import { Content } from '../../../shared/styled'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'
import { VilkaarsType } from '../../../store/reducers/BehandlingReducer'
import { AvdoedesForutMedlemskap } from './vilkaar/AvdoedesForutMedlemskap'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving
  console.log(ctx.state.behandlingReducer)

  if (vilkaar.length === 0) {
    return <div>Mangler vilkår</div>
  }

  return (
    <Content>
      <AlderBarn vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)} />
      <DoedsFallForelder vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)} />
      <AvdoedesForutMedlemskap
        vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.AVDOEDES_FORUTGAAENDE_MELDLEMSKAP)}
      />
    </Content>
  )
}
