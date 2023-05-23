import React, { ReactNode, useState } from 'react'
import { Collapse, Expand } from '@navikt/ds-icons'
import { Heading } from '@navikt/ds-react'
import AnimateHeight from '@navikt/ds-react/esm/util/AnimateHeight'
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
  const { children, topSummary, title, feilBorder, titleHeadingLevel, defaultOpen, ...rest } = props
  const [open, setOpen] = useState<boolean>(defaultOpen)
  const topContent = topSummary instanceof Function ? topSummary(open) : topSummary
  return (
    <PeriodeAccordionWrapper {...rest} feilBorder={feilBorder}>
      <PeriodeAccordionHead>
        <ExpandButton onClick={() => setOpen((o) => !o)} aria-expanded={open}>
          {open ? <Collapse fontSize={20} aria-hidden /> : <Expand fontSize={20} aria-hidden />}
          <Heading size="small" level={titleHeadingLevel}>
            {title}
          </Heading>
        </ExpandButton>
        <div>{topContent}</div>
      </PeriodeAccordionHead>
      <AnimateHeight height={open ? 'auto' : 0} duration={250}>
        <PeriodeAccordionContent>{children}</PeriodeAccordionContent>
      </AnimateHeight>
    </PeriodeAccordionWrapper>
  )
}

const PeriodeAccordionWrapper = styled.div<{ feilBorder: boolean }>`
  border: ${(props) => (props.feilBorder ? '2px solid var(--a-border-danger)' : '1px solid')};
  margin: 0 3em 2em 3em;
  padding: 1em 0;

  &:last-child {
    margin-bottom: 0;
  }
`

const PeriodeAccordionHead = styled.div`
  display: grid;
  grid-template-columns: calc(350px + 2em) 1fr;
  padding: 0 3em 0 1em;
`

const PeriodeAccordionContent = styled.div`
  padding: 0 3em;
`

PeriodeAccordion.defaultProps = defaultAccordionProps

const ExpandButton = styled.button.attrs({ type: 'button' })`
  display: flex;
  flex-direction: row;
  background: none;
  border: none;
  cursor: pointer;

  &:hover {
    color: var(--nav-blue);
  }

  * {
    margin: auto 0;
  }
`

export default PeriodeAccordion
