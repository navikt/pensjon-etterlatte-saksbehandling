import { useState } from "react";
import { Expand } from '@navikt/ds-icons';
import { Link } from "react-router-dom";
import styled from "styled-components";
import { StatusIcon } from "../../../shared/icons/statusIcon";
import { OppfyltIcon } from "../../../shared/icons/oppfyltIcon";
import { upperCaseFirst } from "../../../utils";
import { IVilkaarProps } from "./types";

export const Vilkaar = (props: IVilkaarProps) => {
    const [open, setOpen] = useState(false);

    const toggle = () => {
      setOpen(!open)
    };

    return (
        <div style={{borderBottom: '1px solid #ccc'}}>
            <VilkaarWrapper>
                <div className="flex-width">
                    <StatusIcon status={props.vilkaar.vilkaarDone} />
                    <div className="padding">{props.vilkaar.vilkaarType}</div>
                </div>
                <Toggle onClick={toggle}>§ 15.1 <Expand className="expand" style={{ transform: open? "rotate(180deg)" : ""}} /></Toggle>
                <div className="flex-width">
                    <OppfyltIcon status={props.vilkaar.vilkaarStatus} />
                    <div className="padding">{upperCaseFirst(props.vilkaar.vilkaarStatus)}</div>
                </div>
                <div>
                    <Link to="/rediger">Rediger</Link>
                    <Link to="/slett" style={{ paddingLeft: "5px" }}>
                        Slett
                    </Link>
                </div>
            </VilkaarWrapper>
            {open && <VilkaarContent>Test test</VilkaarContent>}
        </div>
    );
};

const VilkaarWrapper = styled.div`
    height: 100px;
    padding: 1em 1em 1em 0;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    .padding {
        padding: 0 1em;
    }

    .flex-width {
        width: 200px;
        display: flex;
    }

    .expand{
      color: #000;
    }
`;

const Toggle = styled.div`
    cursor: pointer;
    color: #59514B;t
    text-decoration: underline;
`;

const VilkaarContent = styled.div``;
