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
  flex-wrap: wrap;
  gap: 20px;
`

export const InfoList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1rem;
`

export const InfoElement = styled.div`
  width: 15em;
`

export const Undertekst = styled(BodyShort)<{ $gray: boolean }>`
  ${(props) => (props.$gray ? 'color: var(--navds-semantic-color-text-muted)' : null)};
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
  border-top: 1px solid #ccc;
  margin-bottom: 1em;
`

export const VurderingsContainerWrapper = styled(VurderingsContainer)`
  padding-left: 20px;
  width: 10em;
`

export const Beskrivelse = styled.div`
  margin: 10px 0;
  max-width: 41em;
`
