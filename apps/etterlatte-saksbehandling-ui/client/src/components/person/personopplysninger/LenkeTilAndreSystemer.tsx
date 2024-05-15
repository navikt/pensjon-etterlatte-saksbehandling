import React, { ReactNode, useContext } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ExternalLinkIcon, MonitorIcon } from '@navikt/aksel-icons'
import { Link, Tag } from '@navikt/ds-react'
import { SpaceChildren } from '~shared/styled'
import { ConfigContext } from '~clientConfig'

export const LenkeTilAndreSystemer = ({ fnr }: { fnr: string }): ReactNode => {
  const configContext = useContext(ConfigContext)

  return (
    <Personopplysning heading="Snarvei til brukers opplysninger i andre system" icon={<MonitorIcon />}>
      <SpaceChildren direction="row" gap="2rem">
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Go
          </Tag>
          <Link href={`${configContext['gosysUrl']}/personoversikt/fnr=${fnr}`} target="_blank">
            Oversikt i Gosys <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Py
          </Tag>
          <Link href={`${configContext['psakUrl']}/brukeroversikt/fnr=${fnr}`} target="_blank">
            Oversikt i Pesys <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
        <SpaceChildren direction="row" gap="0.5rem">
          <Tag variant="alt3" size="small">
            Mo
          </Tag>
          <Link href={`${configContext['modiapersonoversiktUrl']}/person/${fnr}`} target="_blank">
            Oversikt i Modia <ExternalLinkIcon />
          </Link>
        </SpaceChildren>
      </SpaceChildren>
    </Personopplysning>
  )
}
