import styled from "styled-components";
import { GenderIcon, GenderList } from "../../shared/icons/genderIcon";
import { Fnr } from "./fnr";


enum PersonStatus {
    DØD = "død",
    LEVENDE = "levende",
    ETTERLATT = "etterlatt",
}

interface IStatus {
    status: PersonStatus;
    dato: String;
}

const Status = (props: { value: IStatus }) => {
    return <div>{props.value.status}</div>;
};

export const StatusBar = () => {
    return (
        <StatusBarWrapper>
            <UserInfo>
                <GenderIcon gender={GenderList.female} />
                <Name>Lille My</Name>
                <Fnr copy value={"815493 00134"} />
                <Status value={{ status: PersonStatus.DØD, dato: "19.05.2011" }} />
            </UserInfo>
        </StatusBarWrapper>
    );
};

const StatusBarWrapper = styled.div`
    background-color: #fff;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    padding: 1em;
    line-height: 30px;
`;

const UserInfo = styled.div`
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    width: 300px;
`;

const Name = styled.div`
    font-weight: 600;
    margin-right: auto;
    margin-left: 0.5em;
`;
