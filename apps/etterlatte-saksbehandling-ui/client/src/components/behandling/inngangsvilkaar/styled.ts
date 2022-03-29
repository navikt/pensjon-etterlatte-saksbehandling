import styled from 'styled-components'

export const VilkaarBorder = styled.div`
  padding: 1em 0;
  border-bottom: 1px solid #ccc;
  left: 0;
  right: 0;
`

export const Innhold = styled.div`
  padding: 0 2em;
`

export const VilkaarWrapper = styled.div`
  padding: 1em 1em 1em 0;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: top;
`

export const StatusColumn = styled.div`
  flex: 1;
  flex-grow: 0;
`

export const Title = styled.div`
  display: flex;
  font-size: 1.2em;
  font-weight: bold;
`
export const Lovtekst = styled.div`
  display: flex;
`

export const VilkaarInfobokser = styled.div`
  display: flex;
  flex-direction: row;

  align-items: top;
  flex-grow: 1;
  flex-wrap: wrap;
  gap: 20px;
  padding-right: 20px;
`

export const VilkaarColumn = styled.div`
  width: calc((100% / 3) - (40px / 3));

  @media (max-width: 1600px) {
    width: calc((100% / 2) - (20px / 2));
  }

  @media (max-width: 1300px) {
    width: 100%;
  }

  .missing {
    color: red;
  }
`

export const VilkaarVurderingColumn = styled.div`
  flex: 1;
  min-width: 300px;
  flex-grow: 0;
`

export const VilkaarlisteTitle = styled.div`
  display: flex;
  font-size: 1.1em;
  font-weight: bold;
`
