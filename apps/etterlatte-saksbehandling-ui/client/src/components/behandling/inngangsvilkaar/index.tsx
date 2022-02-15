import styled from 'styled-components'
import { Content } from '../../../shared/styled'
import { Status, VilkaarStatus, VilkaarType } from './types'
import { Vilkaar } from './vilkaar'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'

export const Inngangsvilkaar = () => {
  const ctx = useContext(AppContext)
  const opplysninger = ctx.state.behandlingReducer.grunnlag

  return (
    <Content>
      <VilkaarListe>
        <Vilkaar
          vilkaar={{
            vilkaarDone: Status.DONE,
            vilkaarType: VilkaarType.doedsdato,
            vilkaarStatus: VilkaarStatus.OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === VilkaarType.doedsdato)
          }}
        />
        <Vilkaar
          vilkaar={{
            vilkaarDone: Status.NOT_DONE,
            vilkaarType: VilkaarType.soeker_foedselsdato,
            vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === VilkaarType.soeker_foedselsdato)
          }}
        />
      </VilkaarListe>
    </Content>
  )
}

const VilkaarListe = styled.div`
  padding: 0 2em;
`
