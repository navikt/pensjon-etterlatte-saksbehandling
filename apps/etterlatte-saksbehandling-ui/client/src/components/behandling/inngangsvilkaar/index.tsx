import styled from 'styled-components'
import { Content } from '../../../shared/styled'
// import { Status, VilkaarStatus, OpplysningsType } from './types'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { AlderBarn } from './vilkaar/AlderBarn'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving;

  return (
    <Content>
      <VilkaarListe>
        <AlderBarn vilkaar={vilkaar.find(vilkaar => vilkaar.navn==="SOEKER_ER_UNDER_20")} />
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
