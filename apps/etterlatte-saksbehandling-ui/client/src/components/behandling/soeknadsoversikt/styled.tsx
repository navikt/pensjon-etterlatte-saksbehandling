import { Heading } from '@navikt/ds-react'
import styled from 'styled-components'

export const Infoboks = styled.div`
  width: calc((100% / 3) - (40px / 3));

  @media (max-width: 1600px) {
    width: calc((100% / 2) - (20px / 2));
  }

  @media (max-width: 1300px) {
    width: 100%;
  }
`

export const InfoElement = styled.div<{ $wide?: boolean }>`
  width: ${(props) => (props.$wide ? '100%' : '15em')};
`

export const VurderingsContainer = styled.div`
  display: flex;
  border-left: 4px solid #e5e5e5;
  min-width: 300px;
`

export const VurderingsTitle = ({ title }: { title: string }) => {
  return (
    <Heading level="3" size="small">
      {title}
    </Heading>
  )
}

export const VurderingsContainerWrapper = styled(VurderingsContainer)`
  padding-left: 20px;
  width: 10em;
`

export const Beskrivelse = styled.div`
  margin: 10px 0;
  max-width: 41em;
`
