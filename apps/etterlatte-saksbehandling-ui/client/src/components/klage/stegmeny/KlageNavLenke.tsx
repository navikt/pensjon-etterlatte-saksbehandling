import { NavLink } from 'react-router-dom'
import { Separator } from '~components/behandling/StegMeny/NavLenke'
import React from 'react'
import classNames from 'classnames'

export const KLAGESTEG = ['formkrav', 'vurdering', 'brev', 'oppsummering'] as const

export type Klagesteg = (typeof KLAGESTEG)[number]

export function KlageNavLenke(props: { path: Klagesteg; description: string; enabled: boolean }) {
  const { path, enabled, description } = props

  return (
    <li className={classNames({ disabled: !enabled })}>
      <NavLink to={path}>{description}</NavLink>
      <Separator aria-hidden="true" />
    </li>
  )
}
