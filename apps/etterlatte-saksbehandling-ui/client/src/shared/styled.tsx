import styled from "styled-components";

export const Container = styled.div`
    max-width: 60em;
    margin: 0 auto;
    padding: 1em;
    position: relative;
`;

export const GridContainer = styled.div`
    display: flex;
    height: 100%;
    min-height: 100vh;
`;

export const Column = styled.div`
    min-width: 300px;
    &:nth-child(2) {
        flex-grow: 1;
        border-right: 1px solid #c6c2bf;
        border-left: 1px solid #c6c2bf;
    }
    &:last-child {
        border-right: none;
    }
`;

export const Content = styled.div`
    margin: 0;
    padding: 0;
`;

export const ContentHeader = styled.div`
    padding: 1em 5em;
`;
