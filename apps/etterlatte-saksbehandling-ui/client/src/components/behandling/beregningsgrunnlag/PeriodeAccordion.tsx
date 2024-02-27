import React, { ReactNode } from 'react'
import { Accordion, Heading } from '@navikt/ds-react'
import styled from 'styled-components'

const defaultAccordionProps = {
  defaultOpen: true,
  feilBorder: false,
}

type PeriodeAccordionProps = {
  children: ReactNode
  title: string
  titleHeadingLevel: '1' | '2' | '3' | '4' | '5' | '6'
  topSummary: ReactNode | ((expanded: boolean) => ReactNode)
} & typeof defaultAccordionProps &
  React.HTMLAttributes<HTMLDivElement>

const PeriodeAccordion = (props: PeriodeAccordionProps) => {
  const { children, title, titleHeadingLevel } = props
  return (
    <AccordionWrapper>
      <Accordion.Item>
        <Accordion.Header>
          <Heading size="small" level={titleHeadingLevel}>
            {title}
          </Heading>
        </Accordion.Header>
        <Accordion.Content>{children}</Accordion.Content>
      </Accordion.Item>
    </AccordionWrapper>
  )
}

const AccordionWrapper = styled(Accordion)`
  margin: 0 3em 0 3em;
`

export default PeriodeAccordion
