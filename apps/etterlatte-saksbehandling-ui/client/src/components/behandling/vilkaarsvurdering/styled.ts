import styled from 'styled-components'

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
  height: fit-content;

  .missing {
    color: red;
  }
`

export const VilkaarVurderingColumn = styled.div`
  flex: 1;
  min-width: 300px;
  flex-grow: 0;
`

export const VilkaarVurderingContainer = styled.div`
  border-left: 4px solid #e5e5e5;
`

export const VilkaarlisteTitle = styled.div`
  display: flex;
  font-size: 1.2em;
  font-weight: bold;
  margin-left: 15px;
`
