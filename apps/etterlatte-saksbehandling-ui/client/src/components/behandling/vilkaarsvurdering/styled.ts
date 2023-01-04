import styled from 'styled-components'

export const VilkaarBorder = styled.div`
  padding: 1em 0;
  border-bottom: 1px solid #ccc;
  left: 0;
  right: 0;
`

export const VilkaarBorderTop = styled.div`
  padding: 1em 0;
  border-top: 1px solid #ccc;
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

export const Title = styled.div`
  display: flex;
  font-size: 1em;
  font-weight: bold;
`
export const Lovtekst = styled.div`
  display: flex;
  max-width: 300px;
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
