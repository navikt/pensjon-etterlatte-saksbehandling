import styled from "styled-components";
import { upperCaseFirst } from "../../utils";
import { PersonStatus } from "../behandling/types";

export interface IStatus {
    status: PersonStatus;
    dato: String;
}

export const Status = (props: { value: IStatus }) => {
    return (
        <StatusWrap>
            {upperCaseFirst(props.value.status)} {props.value.dato}
        </StatusWrap>
    );
};

const StatusWrap = styled.div`
    font-size: 0.6em;
    background: #f1f1f1;
    border: 1px solid #3e3832;
    padding: 0 0.6em;
    border-radius: 5px;
    height: 23px;
    align-self: baseline;
    line-height: 23px;
    margin: 0 0.4em;
`;
