import styled from 'styled-components'

export const Infoboks = styled.div`
  width: calc((100% / 3) - (40px / 3));

  @media (max-width: 1600px) {
    width: calc((100% / 2) - (20px / 2));
  }

  @media (max-width: 1300px) {
    width: 100%;
  }
`

export const InfoElement = styled.div<{ $wide?: boolean }>`
  width: ${(props) => (props.$wide ? '100%' : '15em')};
`

export const Vurdering = styled.div`
  padding: 0 0 0 var(--a-spacing-3);
  min-width: 18.75rem;
  width: 10rem;
  border-left: 2px solid var(--a-border-subtle);
`

export const Informasjon = styled.div`
  margin: var(--a-spacing-3) 0;
  max-width: 41rem;
  white-space: pre-wrap;
`
