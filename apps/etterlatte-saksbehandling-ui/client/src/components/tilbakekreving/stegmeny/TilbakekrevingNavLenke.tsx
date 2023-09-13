import { NavLink } from 'react-router-dom'
import { Separator } from '~components/behandling/StegMeny/NavLenke'
import React from 'react'
import classNames from 'classnames'

export const TILBAKEKREVINGSTEG = ['oversikt', 'vurdering', 'vedtak', 'brev'] as const

export type Tilbakekrevingsteg = (typeof TILBAKEKREVINGSTEG)[number]

export function TilbakekrevingNavLenke(props: { path: Tilbakekrevingsteg; description: string; enabled: boolean }) {
  const { path, enabled, description } = props

  return (
    <li className={classNames({ disabled: !enabled })}>
      <NavLink to={path}>{description}</NavLink>
      <Separator aria-hidden="true" />
    </li>
  )
}
