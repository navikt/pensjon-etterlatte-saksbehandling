import styled from "styled-components";
import { GenderIcon, GenderList } from "../../shared/icons/genderIcon";
import { Fnr } from "./fnr";
import { PersonStatus, Status } from "./status";


export enum StatusBarTheme {
    gray = "gray",
    white = "white",
}

export const StatusBar = (props: { theme?: StatusBarTheme }) => {
    return (
        <StatusBarWrapper theme={props.theme}>
            <UserInfo>
                <GenderIcon gender={GenderList.female} />
                <Name>Lille My</Name>
                <Fnr copy value={"815493 00134"} />
                <Status value={{ status: PersonStatus.ETTERLATT, dato: "19.05.2011" }} />
            </UserInfo>
        </StatusBarWrapper>
    );
};

const StatusBarWrapper = styled.div<{ theme: StatusBarTheme }>`
    background-color: ${(props) => (props.theme === StatusBarTheme.gray ? "#F8F8F8" : "#fff")};
    padding: 0.6em 1em;
    line-height: 30px;
`;

const UserInfo = styled.div`
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: baseline;
    width: 400px;
`;

const Name = styled.div`
    font-weight: 600;
    margin-right: auto;
    margin-left: 0.5em;
`;
