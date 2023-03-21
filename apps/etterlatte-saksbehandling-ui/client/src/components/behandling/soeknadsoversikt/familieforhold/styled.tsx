import styled from 'styled-components'

export const PersonBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
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

export const TableWrapper = styled.div`
  td:first-child,
  th:first-child {
    padding-left: 2.5rem;
  }
`

export const IconWrapper = styled.span`
  width: 2.5rem;
`

export const FlexHeader = styled.div`
  display: flex;
  margin-bottom: 1rem;
`
