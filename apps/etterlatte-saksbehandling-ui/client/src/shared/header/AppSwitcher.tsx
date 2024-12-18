import { Dropdown, InternalHeader } from '@navikt/ds-react'
import { ExternalLinkIcon, MenuGridIcon } from '@navikt/aksel-icons'
import React, { useContext } from 'react'
import { ConfigContext } from '~clientConfig'

export const AppSwitcher = () => {
  const configContext = useContext(ConfigContext)

  return (
    <Dropdown>
      <InternalHeader.Button as={Dropdown.Toggle}>
        <MenuGridIcon style={{ fontSize: '1.5rem' }} title="Systemer og oppslagsverk" />
      </InternalHeader.Button>
      <Dropdown.Menu>
        <Dropdown.Menu.GroupedList>
          <Dropdown.Menu.GroupedList.Heading>Systemer og oppslagsverk</Dropdown.Menu.GroupedList.Heading>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['gosysUrl']}>
            Gosys <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['eessiPensjonUrl']}>
            EESSI <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['rinaUrl']}>
            RINA <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['psakUrl']}>
            PSAK <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['modiapersonoversiktUrl']}>
            Modia <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
          <Dropdown.Menu.GroupedList.Item as="a" target="_blank" href={configContext['bisysUrl']}>
            Bisys <ExternalLinkIcon aria-hidden />
          </Dropdown.Menu.GroupedList.Item>
        </Dropdown.Menu.GroupedList>
      </Dropdown.Menu>
    </Dropdown>
  )
}
