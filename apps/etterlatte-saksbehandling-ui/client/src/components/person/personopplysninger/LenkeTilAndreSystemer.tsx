import React, { ReactNode, useContext } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ExternalLinkIcon, MonitorIcon } from '@navikt/aksel-icons'
import { HStack, Link, Tag } from '@navikt/ds-react'
import { ConfigContext } from '~clientConfig'

export const LenkeTilAndreSystemer = ({ fnr }: { fnr: string }): ReactNode => {
  const configContext = useContext(ConfigContext)

  return (
    <Personopplysning heading="Snarvei til brukers opplysninger i andre system" icon={<MonitorIcon />}>
      <HStack gap="8">
        <HStack gap="2">
          <Tag variant="alt3" size="small">
            Go
          </Tag>
          <Link href={`${configContext['gosysUrl']}/personoversikt/fnr=${fnr}`} target="_blank">
            Oversikt i Gosys <ExternalLinkIcon />
          </Link>
        </HStack>
        <HStack gap="2">
          <Tag variant="alt3" size="small">
            Py
          </Tag>
          <Link href={`${configContext['psakUrl']}/brukeroversikt/fnr=${fnr}`} target="_blank">
            Oversikt i Pesys <ExternalLinkIcon />
          </Link>
        </HStack>
        <HStack gap="2">
          <Tag variant="alt3" size="small">
            Mo
          </Tag>
          <Link href={`${configContext['modiapersonoversiktUrl']}/sak/${fnr}`} target="_blank">
            Oversikt i Modia <ExternalLinkIcon />
          </Link>
        </HStack>
      </HStack>
    </Personopplysning>
  )
}
