import { VilkaarWrapper, Title } from './styled'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import { Status, VilkaarType } from './types'
import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'

const Doedsdato = () => {
  const ctx = useContext(AppContext)
  console.log('TEST', ctx.state.behandlingReducer)
  const opplysninger = ctx.state.behandlingReducer.grunnlag
  const doesdato = opplysninger.filter((opplysning) => opplysning.opplysningsType === VilkaarType.doedsdato.toString())
  console.log(doesdato)

  return (
    <div style={{ borderBottom: '1px solid #ccc' }}>
      <VilkaarWrapper>
        <Title>
          <StatusIcon status={Status.DONE} /> Dødsfall forelder
        </Title>
        <div>MEr info her</div>
      </VilkaarWrapper>
    </div>
  )
}

export default Doedsdato
