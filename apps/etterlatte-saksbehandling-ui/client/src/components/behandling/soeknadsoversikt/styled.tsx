import styled from 'styled-components'

export const InfoWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 30px;
  border-top: 1px solid #b0b0b0;
  padding: 1.2em 1em 2em 1em;
  width: 100%;
`

export const DetailWrapper = styled.div`
  padding-top: 0.5em;
  margin-bottom: 2em;
  width: 33%;
  font-size: 16px;
`

export const WarningText = styled.span`
  color: #ba3a26;
  display: block;
`

export const Border = styled.div`
  border-top: 1px solid #b0b0b0;
`

export const PersonInfoWrapper = styled.div`
  border-bottom: 1px solid #b0b0b0;
  padding: 1.2em 1em 2em 0em;

  .personWrapper {
    width: 90%;
    display: inline-flex;
    padding-top: 1em;
    margin-right: 0em;
  }
`

export const AlertWrapper = styled.div`
  width: 50%;
  .alert {
    max-width: 300px;
    padding: 1em;
  }
`

export const PersonDetailWrapper = styled.div`
  padding-top: 0.5em;
  padding-left: 1em;
  width: 250px;

  .bodyShortHeading {
    font-weight: bold;
  }
  .adresse {
    display: inline-flex;
    white-space: nowrap;
  }
`

export const Historikk = styled.div`
  display: block;
  padding-left: 1.5em;
`

export const HistorikkWrapper = styled.div`
  font-size: 11px;
  width: 400px;
`

export const HistorikkElement = styled.div`
  font-size: 16px;
  display: flex;
  flex-wrap: wrap;

  .date {
    margin-right: 1em;
    width: 10em;
  }
`

export const PersonInfoHeader = styled.div`
  display: inline-flex;
  font-weight: bold;

  .personRolle {
    font-weight: normal;
    margin-left: 0.5em;
  }
`

export const HeadingWrapper = styled.div`
  display: inline-flex;

  .details {
    justify-content: center;
    align-item: center;
    padding: 0.2em;
  }
`

export const TextButtonWrapper = styled.div`
margin-right: 5em;
margin-bottom: 0;

.textButton{
  margin-bottom: 0;
  display: inline-flex;
  justify-content: space-between;
  color: #0067c5;
  :hover {
    cursor: pointer;
  }
  .dropdownIcon {
    margin-bottom: 0;
    margin-left: 0.5em;
    margin-top 0.1em;
 
  }
}
`

export const StatsborgerskapWrap = styled.div`
  background-color: #ffeccc;
  border: 1px solid #ffc166;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  margin-left: 0.7em;
  margin-right: 0.5em;
`
export const AlderEtterlattWrap = styled.div`
  background-color: #ccf1d6;
  border: 1px solid #33aa5f;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  margin-left: 0.7em;
  margin-right: 0.5em;
`

export const RadioGroupWrapper = styled.div`
  margin-top: 5em;
  max-width: 400px;

  .link {
    font-weight: bold;
    margin-top: 1em;
    margin-left: 1.5em;
  }
  .radioGroup {
    margin-bottom: 1em;
  }
  .button {
    margin-top: 1.5em;
    padding: 0.5em 3em;
  }
`
