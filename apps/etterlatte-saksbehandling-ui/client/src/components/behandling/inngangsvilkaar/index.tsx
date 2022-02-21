import styled from 'styled-components'
import { Content } from '../../../shared/styled'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'
import { VilkaarsType } from '../../../store/reducers/BehandlingReducer'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving
  console.log(ctx.state.behandlingReducer)

  if (vilkaar.length === 0) {
    return <div>Mangler vilkår</div>
  }

  return (
    <Content>
      <VilkaarListe>
        <AlderBarn vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SOEKER_ER_UNDER_20)} />
        <DoedsFallForelder vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)} />
        {/*
          <AvdoedesForutMedlemskap
          vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.AVDOEDES_FORUTGAAENDE_MELDLEMSKAP)}
        />
        <Vilkaar
          vilkaar={}
        />
        <Vilkaar
          vilkaar={{
            vilkaarDone: Status.NOT_DONE,
            vilkaarType: OpplysningsType.avdoedes_forutgaaende_medlemsskap,
            vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === OpplysningsType.avdoedes_forutgaaende_medlemsskap)
          }}
        />
        */}
      </VilkaarListe>
    </Content>
  )
}

const VilkaarListe = styled.div`
  padding: 0 2em;
`
