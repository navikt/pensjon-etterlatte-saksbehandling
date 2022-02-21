import styled from 'styled-components'
import { Content } from '../../../shared/styled'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving
  
  if(vilkaar.length === 0) {
    return <div>Mangler vilkår</div>;
  }

  return (
    <Content>
      <VilkaarListe>
        <AlderBarn vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === 'SOEKER_ER_UNDER_20')} />
        <DoedsFallForelder vilkaar={vilkaar.find((vilkaar) => vilkaar.navn === 'DOEDSFALL_ER_REGISTRERT')} />
        {/*
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
