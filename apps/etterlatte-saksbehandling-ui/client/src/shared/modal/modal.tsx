import { ReactNode } from "react";
import styled from "styled-components";
import { CloseIcon } from "../icons/closeIcon";
import { Portal } from "../portal";

export const Modal = (props: { onClose: any; children: ReactNode }) => {
    return (
        <Portal>
            <ModalWrapper>
                <ModalBox>
                    <Close onClick={props.onClose}><CloseIcon /></Close>
                    <div>{props.children}</div>
                </ModalBox>
            </ModalWrapper>
        </Portal>
    );
};

const ModalWrapper = styled.div`
    position: absolute;
    background-color: rgba(0, 0, 0, 0.7);
    left: 0;
    right: 0;
    bottom: 0;
    top: 0;
    z-index: 10;
`;

const ModalBox = styled.div`
    background-color: #fff;
    border: 1px solid #333;
    position: absolute;
    max-width: 600px;
    height: 400px;
    margin: auto;
    left: 0;
    right: 0;
    bottom: 0;
    top: 0;
    padding: 2em;
    display: flex;
    flex-direction: column;
`;

const Close = styled.div`
    align-self: flex-end;
    cursor: pointer;
`;
