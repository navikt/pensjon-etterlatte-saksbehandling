import { ExpansionCard, HStack, Link, Tag } from '@navikt/ds-react'
import { ExternalLinkIcon, MonitorIcon } from '@navikt/aksel-icons'
import React, { useContext } from 'react'
import { ConfigContext } from '~clientConfig'

export const BrukersOpplysningerIAndreSystemerExpansionCard = ({ fnr }: { fnr: string }) => {
  const configContext = useContext(ConfigContext)

  return (
    <ExpansionCard aria-labelledby="Brukers opplysninger i andre systemer" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <MonitorIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">Brukers opplysninger i andre systemer</ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <HStack gap="space-8">
          <HStack gap="space-2">
            <Tag data-color="info" variant="outline" size="small">
              Go
            </Tag>
            <Link href={`${configContext['gosysUrl']}/personoversikt/fnr=${fnr}`} target="_blank">
              Oversikt i Gosys <ExternalLinkIcon aria-hidden />
            </Link>
          </HStack>
          <HStack gap="space-2">
            <Tag data-color="info" variant="outline" size="small">
              Py
            </Tag>
            <Link href={`${configContext['psakUrl']}/brukeroversikt/fnr=${fnr}`} target="_blank">
              Oversikt i Pesys <ExternalLinkIcon aria-hidden />
            </Link>
          </HStack>
          <HStack gap="space-2">
            <Tag data-color="info" variant="outline" size="small">
              Mo
            </Tag>
            <Link href={`${configContext['modiapersonoversiktUrl']}/person/${fnr}`} target="_blank">
              Oversikt i Modia <ExternalLinkIcon aria-hidden />
            </Link>
          </HStack>
        </HStack>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
