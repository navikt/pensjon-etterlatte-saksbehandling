import { BodyShort } from '@navikt/ds-react'
import styled from 'styled-components'

export const Innhold = styled.div`
  padding: 2em 2em 2em 4em;
`

export const SoeknadOversiktWrapper = styled.div`
  padding: 1em 1em 1em 0em;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: top;
`

export const Header = styled.div`
  font-size: 1.2em;
  font-weight: bold;
  margin-bottom: 1em;
  margin-top: 0em;
`

export const InfobokserWrapper = styled.div`
  display: flex;
  flex-direction: row;
  align-items: top;
  flex-grow: 1;
  flex-wrap: wrap;
  gap: 20px;
  padding-right: 20px;
`

export const Infoboks = styled.div`
  width: calc((100% / 3) - (40px / 3));

  @media (max-width: 1600px) {
    width: calc((100% / 2) - (20px / 2));
  }

  @media (max-width: 1300px) {
    width: 100%;
  }
`

export const InfoWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);

  > * {
    width: 180px;
  }

  flex-grow: 1;
`

export const Undertekst = styled(BodyShort)<{ $gray: boolean }>`
  ${(props) => (props.$gray ? 'color: var(--navds-semantic-color-text-muted)' : null)};
`

export const DetailWrapper = styled.div`
  font-size: 16px;
  width: 150px;
  margin-bottom: 2em;

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

export const VurderingsWrapper = styled.div`
  flex: 1;
  flex-grow: 0;
  min-width: 300px;
`

export const VurderingsContainer = styled.div`
  display: flex;
  border-left: 4px solid #e5e5e5;
  min-width: 300px;
`

export const VurderingsTitle = styled.div`
  display: flex;
  font-size: 1.1em;
  font-weight: bold;
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
  margin-bottom: 1em;
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
  display: inline-block;
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
  display: inline-block;
`
