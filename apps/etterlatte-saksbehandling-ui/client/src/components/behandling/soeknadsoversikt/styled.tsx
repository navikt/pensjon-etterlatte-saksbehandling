import { BodyShort, Heading } from '@navikt/ds-react'
import styled from 'styled-components'

export const Innhold = styled.div`
  padding: 2em 2em 2em 4em;
`

export const SoeknadOversiktWrapper = styled.div`
  padding: 0em 1em 1em 0em;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: top;
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
  display: flex;
  flex-grow: 1;
  flex-wrap: wrap;
`

export const InfoElement = styled.div`
  min-width: 10em;
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

export const VurderingsTitle = ({ title }: { title: string }) => {
  return (
    <Heading level={'3'} size={'small'}>
      {title}
    </Heading>
  )
}

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
  margin-top: 3em;

  .details {
    margin-bottom: 0.6em;
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

export const VurderingsContainerWrapper = styled(VurderingsContainer)`
  padding-left: 20px;
`

export const Beskrivelse = styled.div`
  margin: 10px 0;
  max-width: 41em;
`
