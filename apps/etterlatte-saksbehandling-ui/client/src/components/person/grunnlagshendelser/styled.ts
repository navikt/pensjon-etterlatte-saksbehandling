import styled from 'styled-components'

export const HendelseSammenligning = styled.dl`
  display: grid;
  width: 100%;
  grid-template-columns: repeat(2, auto);
  grid-gap: 5px;

  p {
    margin: 0;
  }
`

// TODO: Hele dette designet må tas en avsjekk med fag / design om det
//      1. dekker behovet for å se relevante opplysninger
//      2. må fikses så det ikke ser helt løk ut
export const HendelseWrapper = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 5px;
  background-color: #f5f5f5;
  margin-bottom: 30px;
`

export const HendelseMetaWrapper = styled.div`
  display: flex;
  flex-direction: row;
  width: 100%;
  border-bottom: 1px solid black;
`

export const HendelseMetaItem = styled.div`
  display: inline-block;
  width: 33%;
  padding-right: 10px;
  padding: 5px;
  font-weight: bold;

  small {
    font-weight: normal;
    font-size: 14px;
  }
`
