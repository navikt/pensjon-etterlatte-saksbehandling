import styled from "styled-components";

export const Container = styled.div`
    max-width: 60em;
    margin: 0 auto;
    padding: 1em;
    position: relative;
`;

export const GridContainer = styled.div`
    display: grid;
    grid-template-columns: 1fr 3fr 1fr;
    height: 100vh;
`;

export const Column = styled.div`
    border-top: 1px solid #c6c2bf;
    &:nth-child(2) {
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
    padding: 1em;
`;