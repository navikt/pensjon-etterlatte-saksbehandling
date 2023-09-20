import { NavLink } from 'react-router-dom'
import { Separator } from '~components/behandling/StegMeny/NavLenke'
import React from 'react'
import classNames from 'classnames'

export const TILBAKEKREVINGSTEG = ['oversikt', 'vurdering', 'vedtak', 'brev'] as const

export type Tilbakekrevingsteg = (typeof TILBAKEKREVINGSTEG)[number]

export interface TilbakekrevingNavLenkeProps {
  path: Tilbakekrevingsteg
  description: string
  enabled: boolean
  separator: boolean
}

export function TilbakekrevingNavLenke(props: TilbakekrevingNavLenkeProps) {
  const { path, description, enabled, separator } = props

  return (
    <li className={classNames({ disabled: !enabled })}>
      <NavLink to={path}>{description}</NavLink>
      {separator && <Separator aria-hidden="true" />}
    </li>
  )
}
