import styled from 'styled-components'

export const PersonBorder = styled.div`
  border-bottom: 1px solid #b0b0b0;
  padding: 1.2em 1em 3em 0em;
`

export const IconWrapper = styled.span`
  margin-left: -40px;
  margin-right: 20px;
`
export const PersonInfoWrapper = styled.div`
  padding-top: 1.2em;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
    margin-right: 0.5em;
  }
`
