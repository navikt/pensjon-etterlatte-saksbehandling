import styled from 'styled-components'

export const Innhold = styled.div`
  padding: 0 2em;
`

export const VilkaarWrapper = styled.div`
  padding: 1em 1em 1em 2em;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: top;
`

export const Title = styled.div`
  display: flex;
  font-size: 1.1em;
  font-weight: bold;
  margin-bottom: 5px;
`
export const VilkaarBeskrivelse = styled.div`
  margin-top: 15px;
  max-width: 600px;
  display: flex;
  white-space: pre-wrap;
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
