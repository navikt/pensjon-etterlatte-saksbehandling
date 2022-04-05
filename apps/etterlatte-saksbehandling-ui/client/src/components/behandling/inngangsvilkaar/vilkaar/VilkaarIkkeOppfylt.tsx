import styled from 'styled-components'
import { CloseIcon } from '../../../../shared/icons/closeIcon'
import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export const VilkaarIkkeOppfylt = (props: { status: VurderingsResultat; errorText: string }) => {
  if (props.status === VurderingsResultat.OPPFYLT) {
    return <></>
  }
  if (props.status === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
    return (
      <VilkaarError>
        <Error>
          <CloseIcon color="#fff" />
        </Error>
        <ErrorText>{props.errorText}</ErrorText>
      </VilkaarError>
    )
  }

  return (
    <VilkaarError>
      <Error>
        <CloseIcon color="#fff" />
      </Error>
      <ErrorText>{props.errorText}</ErrorText>
    </VilkaarError>
  )
}

const VilkaarError = styled.div`
  width: 100%;
  height: 100px;
  background-color: var(--nav-error-bg);
  border: 1px solid var(--nav-error-border);
  padding: 1em;
  display: flex;
  border-radius: 5px;
`

const Error = styled.div`
  background-color: var(--nav-error-border);
  width: 50px;
  height: 50px;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 100%;
  flex-shrink: 0;
`

const ErrorText = styled.div`
  padding: 0 1em;
`
