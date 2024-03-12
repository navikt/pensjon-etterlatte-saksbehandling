import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ExternalLinkIcon, MonitorIcon } from '@navikt/aksel-icons'
import { Link, Tag } from '@navikt/ds-react'
import { SpaceChildren } from '~shared/styled'

export const LenkeTilAndreSystemer = (): ReactNode => {
  return (
    <Personopplysning
      heading="Snarvei til brukers opplysninger i andre system"
      icon={<MonitorIcon height="2rem" width="2rem" />}
    >
      <SpaceChildren direction="row" gap="2rem">
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Go
          </Tag>
          <Link href="#">
            Oversikt i Gosys <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Py
          </Tag>
          <Link href="#">
            Oversikt i Pesys <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Mo
          </Tag>
          <Link href="#">
            Oversikt i Modia <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
      </SpaceChildren>
    </Personopplysning>
  )
}
