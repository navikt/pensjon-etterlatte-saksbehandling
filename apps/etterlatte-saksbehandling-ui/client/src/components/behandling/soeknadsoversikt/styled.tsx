import styled from 'styled-components'

export const InfoWrapper = styled.div`
  display: grid;
  grid-gap: 10px;
  grid-template-columns: repeat(3, 1fr);
`

export const DetailWrapper = styled.div`
  font-size: 16px;
  min-width: 200px;
  height: 100px;

  .warningText {
    color: #ba3a26;
    max-width: 150px;
    display: block;
  }
  .text {
    max-width: 150px;
  }
`

export const Border = styled.div`
  border-top: 1px solid #b0b0b0;
`

export const PersonInfoBorder = styled.div`
  border-bottom: 1px solid #b0b0b0;
  padding: 1.2em 1em 3em 0em;
`

export const PersonInfoWrapper = styled.div`
  padding-top: 1.2em;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  flex-wrap: wrap;
`
export const PersonDetailWrapper = styled.div<{ adresse: boolean }>`
  padding-top: 0.5em;
  padding-left: 1em;
  min-width: ${(props) => (props.adresse ? '400px' : '150px')};

  .bodyShortHeading {
    margin-bottom: 0.2em;
    font-weight: bold;
  }
`

export const AlertWrapper = styled.div`
  min-width: 200px;
  max-width: 350px;

  .alert {
    font-size: 10px;
    padding: 1em;
  }
`

export const Historikk = styled.div`
  padding-top: 0.5em;
`

export const HistorikkElement = styled.div`
  font-size: 16px;
  display: flex;
  white-space: nowrap;

  .date {
    margin-right: 1em;
    width: 10em;
  }
`

export const PersonInfoHeaderWrapper = styled.div`
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

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    justify-content: center;
    align-item: center;
    padding: 0.6em;
  }
`

export const TextButtonWrapper = styled.div`

.textButton{
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
  margin-right: 0.5em;
`
export const BarnAlderWrap = styled.div`
  background-color: #ccf1d6;
  border: 1px solid #33aa5f;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  margin-right: 1em;
`

export const AvdoedWrap = styled.div`
  background-color: #262626;
  color: white;
  border: 1px solid #262626;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  margin-left: 0.7em;
  margin-right: 0.9em;
`

export const RadioGroupWrapper = styled.div`
  margin-top: 3em;
  max-width: 400px;

  .textarea {
    margin-top: 1em;
  }
}
`

export const IconWrapper = styled.span`
  margin-left: -40px;
  margin-right: 20px;
`
