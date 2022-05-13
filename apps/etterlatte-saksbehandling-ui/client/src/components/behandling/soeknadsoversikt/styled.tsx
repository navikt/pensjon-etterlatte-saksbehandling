import styled from 'styled-components'

export const DetailWrapper = styled.div`
  font-size: 16px;
  width: 150px;
  height: 100px;

  .warningText {
    color: #ba3a26;
    max-width: 150px;
  }
  .headertext {
    width: 250px;
  }
  .text {
    width: 280px;
  }

  .labelWrapperWithIcon {
    display: flex;
  }
`

export const WarningIconWrapper = styled.div`
  margin-left: -25px;
  margin-right: 5px;
`

export const PersonDetailWrapper = styled.div<{ adresse: boolean }>`
  padding-top: 0.5em;
  min-width: ${(props) => (props.adresse ? '300px' : '100px')};
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
    padding: 0.6em;
  }
`

export const Border = styled.div`
  border-top: 1px solid #b0b0b0;
  margin-bottom: 1em;
`

export const DashedBorder = styled.div`
  background-image: linear-gradient(to right, #000 10%, rgba(255, 255, 255, 0) 0%);
  background-size: 10px 1px;
  background-repeat: repeat-x;
  width: 100%;
  padding: 1px;
`

export const TypeStatusWrap = styled.div<{ type: string }>`
  background-color: ${(props) => (props.type === 'barn' ? '#ccf1d6' : '#ffeccc')};
  border: 1px solid ${(props) => (props.type === 'barn' ? '#33aa5f;' : '#ffc166')};
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  height: 25px;
  margin-right: ${(props) => (props.type === 'barn' ? '1em;' : '0.5em')};
`

export const ForelderWrap = styled.div<{ avdoed?: boolean }>`
  background-color: ${(props) => (props.avdoed ? '#262626' : '#ffeccc')};
  color: ${(props) => (props.avdoed ? '#FFFFFF' : '262626')};
  border: 1px solid ${(props) => (props.avdoed ? '#262626' : '#262626')};
  height: 25px;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  font-weight: normal;
  font-size: 14px;
  margin-right: 0.9em;
`
