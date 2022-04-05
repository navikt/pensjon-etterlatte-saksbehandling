import styled from 'styled-components'

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

  .detailWrapperWithIcon {
    display: flex;
  }
`

export const WarningIconWrapper = styled.div`
  margin-left: -25px;
  margin-right: 5px;
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

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    justify-content: center;
    align-item: center;
    padding: 0.6em;
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
