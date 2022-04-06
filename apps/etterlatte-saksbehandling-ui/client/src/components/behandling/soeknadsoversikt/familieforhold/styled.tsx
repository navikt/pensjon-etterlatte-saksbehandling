import styled from 'styled-components'

export const PersonBorder = styled.div`
  border-top: 1px dashed #000000;
  padding: 1.2em 1em 3em 0em;
`

export const IconWrapper = styled.span`
  margin-left: -55px;
  margin-right: 40px;
`
export const PersonInfoWrapper = styled.div`
  padding-top: 1.2em;
  display: grid;
  gap: 35px;
  grid-template-columns: 2fr 5fr 4fr;

  @media (max-width: 1400px) {
    display: flex;
    flex-wrap: wrap;
  }
`

export const PersonHeader = styled.div`
  display: inline-flex;
  font-weight: bold;
  margin-top: 10px;

  .personRolle {
    font-weight: normal;
    margin-left: 0.5em;
    margin-right: 0.7em;
  }

  .icon {
    margin-left: -2em;
    margin-right: 0.5em;
  }
`
