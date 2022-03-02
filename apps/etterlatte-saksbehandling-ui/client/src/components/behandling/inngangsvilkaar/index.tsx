import { Content } from '../../../shared/styled'
import { useContext, useEffect } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'
import { VilkaarsType } from '../../../store/reducers/BehandlingReducer'
import { AvdoedesForutMedlemskap } from './vilkaar/AvdoedesForutMedlemskap'
import { useLocation } from 'react-router-dom'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const location = useLocation()
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving


  useEffect(() => {
    const hash = location.hash.slice(1);
    console.log(document.getElementById(hash))
    document.getElementById(hash)?.scrollIntoView({behavior: "smooth", block: "end", inline: "nearest"})
  }, [location.hash])

  if (vilkaar.length === 0) {
    return <div>Mangler vilkår</div>
  }

  return (
    <Content>
      <AlderBarn id="alderbarn" vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)} />
      <DoedsFallForelder
        id="dodsfallforelder"
        vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)}
      />
      <AvdoedesForutMedlemskap
        id="avdodesmedlemskap"
        vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.AVDOEDES_FORUTGAAENDE_MELDLEMSKAP)}
      />
    </Content>
  )
}
