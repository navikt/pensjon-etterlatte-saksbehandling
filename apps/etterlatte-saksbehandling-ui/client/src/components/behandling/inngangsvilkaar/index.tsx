import styled from 'styled-components'
import { Content } from '../../../shared/styled'
import { Status, VilkaarStatus, OpplysningsType } from './types'
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
            vilkaarType: OpplysningsType.doedsdato,
            vilkaarStatus: VilkaarStatus.OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === OpplysningsType.doedsdato)
          }}
        />
        <Vilkaar
          vilkaar={{
            vilkaarDone: Status.NOT_DONE,
            vilkaarType: OpplysningsType.soeker_foedselsdato,
            vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === OpplysningsType.soeker_foedselsdato)
          }}
        />
        <Vilkaar
          vilkaar={{
            vilkaarDone: Status.NOT_DONE,
            vilkaarType: OpplysningsType.avdoedes_forutgaaende_medlemsskap,
            vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
            grunnlag: opplysninger.find(opplysning => opplysning.opplysningsType === OpplysningsType.avdoedes_forutgaaende_medlemsskap)
          }}
        />
      </VilkaarListe>
    </Content>
  )
}

const VilkaarListe = styled.div`
  padding: 0 2em;
`
