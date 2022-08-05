import styled from 'styled-components'

export const PersonBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
`

export const IconWrapper = styled.span`
  margin-left: -55px;
  margin-right: 40px;
`
export const PersonInfoWrapper = styled.div`
  padding-top: 1.2em;
  display: inline-flex;
  gap: 100px;
  grid-template-columns: 2fr 5fr 4fr;

  @media (max-width: 1400px) {
    display: flex;
    flex-wrap: wrap;
  }
`

export const PersonHeader = styled.div`
  display: inline-table;
  font-weight: bold;
  margin-top: 10px;
  width: 350px;

  .personRolle {
    font-weight: normal;
    margin-left: 0.5em;
    margin-right: 0.7em;
  }
  
  .personInfo {
    font-weight: normal;
  }

  .icon {
    margin-left: -2em;
    margin-right: 0.5em;
  }
`
