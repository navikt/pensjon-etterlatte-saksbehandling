import React, { ReactNode } from 'react'
import { SakStatus as SakStatusProps } from '~shared/types/sak'
import { SpaceChildren } from '~shared/styled'
import { Variants } from '~shared/Tags'
import { formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
import { Tag } from '@navikt/ds-react'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { CheckmarkIcon, XMarkIcon } from '@navikt/aksel-icons'

const Loepende = (): ReactNode => (
  <Tag variant="success" icon={<CheckmarkIcon aria-hidden />}>
    Løpende
  </Tag>
)

const Avbrutt = (): ReactNode => (
  <Tag variant="error" icon={<XMarkIcon aria-hidden />}>
    Avbrutt
  </Tag>
)

const IkkeIverksatt = (): ReactNode => <Tag variant="neutral">Ikke iverksatt</Tag>

export const SakStatus = ({ behandlingStatus, virkningstidspunkt }: SakStatusProps) => {
  const velgBehandlingStatusTag = () => {
    switch (behandlingStatus) {
      case IBehandlingStatus.IVERKSATT:
        return <Loepende />
      case IBehandlingStatus.SAMORDNET:
        return <Loepende />
      case IBehandlingStatus.AVBRUTT:
        return <Avbrutt />
      default:
        return <IkkeIverksatt />
    }
  }

  return (
    <SpaceChildren direction="row">
      <Tag variant={Variants.NEUTRAL}>
        {formaterEnumTilLesbarString(behandlingStatus)} {!!virkningstidspunkt && formaterStringDato(virkningstidspunkt)}
      </Tag>
      {velgBehandlingStatusTag()}
    </SpaceChildren>
  )
}
