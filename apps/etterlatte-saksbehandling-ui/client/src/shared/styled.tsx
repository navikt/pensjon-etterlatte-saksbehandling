import styled from 'styled-components';


export const Container = styled.div`
  max-width: 60em;
  margin: 0 auto;
  position: relative;
`;

export const GridContainer = styled.div`
    display: grid;
    grid-template-columns: 1fr 2fr 1fr;
    height: 100vh;
`;

export const Column = styled.div`
    border-right: 1px solid #c6c2bf;
    border-top: 1px solid #c6c2bf;
    &:last-child{
      border-right:none;
    }
`;