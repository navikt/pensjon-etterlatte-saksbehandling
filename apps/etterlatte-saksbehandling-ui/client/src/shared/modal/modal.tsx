import { ReactNode } from 'react'
import styled from 'styled-components'
import { Close } from '@navikt/ds-icons'
import { Portal } from '../portal'

export const Modal = (props: { onClose: any; children: ReactNode }) => {
  return (
    <Portal>
      <ModalWrapper>
        <ModalBox>
          <CloseIconWrapper onClick={props.onClose}>
            <Close color={'#0067C5'}/>
          </CloseIconWrapper>
          <div>{props.children}</div>
        </ModalBox>
      </ModalWrapper>
    </Portal>
  )
}

const ModalWrapper = styled.div`
  position: fixed;
  background-color: rgba(0, 0, 0, 0.7);
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  z-index: 10;
`

const ModalBox = styled.div`
  text-align: center;
  border-radius: 4px;
  background-color: #fff;
  border: 2px solid #0067c5;
  position: absolute;
  min-width: fit-content;
  max-width: 600px;
  min-height: fit-content;
  max-height: 500px;
  margin: auto;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  padding: 2em;
  display: flex;
  flex-direction: column;

  .button {
    margin: 0 1em 0.5em 1em;
  }
`

const CloseIconWrapper = styled.div`
  align-self: flex-end;
  cursor: pointer;
`
