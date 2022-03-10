import styled from 'styled-components'

export const VilkaarBorder = styled.div`
  padding: 1em 0;
  border-bottom: 1px solid #ccc;
  left: 0;
  right: 0;
`

export const VilkaarWrapper = styled.div`
  padding: 1em 1em 1em 0;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: top;
  gap: 20px;

  @media (max-width: 1200px) {
    flex-wrap: wrap;
  }
`

export const Title = styled.div`
  display: flex;
  font-size: 1.2em;
  font-weight: bold;
`
export const VilkaarlisteTitle = styled.div`
  display: flex;
  font-size: 1.1em;
  font-weight: bold;
`

export const VilkaarColumn = styled.div`
  flex-basis: 300px;
  flex: 1;
  min-width: 150px;

  .missing {
    color: red;
  }
`

export const VilkaarInfobokser = styled.div`
  display: flex;
  flex-grow: 1;
  flex-wrap: wrap;
  gap: 20px;
`

export const VilkaarVurderingColumn = styled.div`
  flex: 1;
  min-width: 300px;
`

export const Innhold = styled.div`
  padding: 0 2em;
`
