import { Link } from "react-router-dom";
import styled from "styled-components";
import { Status, StatusIcon } from "../../../shared/icons/statusIcon";
import { WorkerIcon } from "../../../shared/icons/workerIcon";
import { upperCaseFirst } from "../../../utils";

export enum VilkaarStatus {
    OPPFYLT = "oppfylt",
    IKKE_OPPFYLT = "ikke oppfylt",
}
export interface IVilkaarProps {
    vilkaar: {
        vilkaarDone: Status;
        vilkaarType: string;
        vilkaarStatus: VilkaarStatus;
    };
}

export const Vilkaar = (props: IVilkaarProps) => {
    return (
        <VilkaarWrapper>
            <div className="flex-width">
                <StatusIcon status={props.vilkaar.vilkaarDone} />
                <div className="padding">{props.vilkaar.vilkaarType}</div>
            </div>
            <div className="flex-width">
                <WorkerIcon />
                <div className="padding">{upperCaseFirst(props.vilkaar.vilkaarStatus)}</div>
            </div>
            <div>
                <Link to="/rediger">Rediger</Link>
                <Link to="/slett" style={{ paddingLeft: "5px" }}>
                    Slett
                </Link>
            </div>
        </VilkaarWrapper>
    );
};

const VilkaarWrapper = styled.div`
    height: 100px;
    border-bottom: 1px solid #ccc;
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
`;
