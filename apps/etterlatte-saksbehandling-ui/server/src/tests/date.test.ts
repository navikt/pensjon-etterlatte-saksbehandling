/* eslint-disable @typescript-eslint/no-unused-expressions */

import { epochToUTC } from '../utils/date'
import { expect } from 'chai'
import { hasBeenIssued, hasExpired } from '../middleware/auth'

describe('Test date functions', () => {
  it('should test epochToUTC', () => {
    expect(epochToUTC(1639405540).toUTCString()).to.equal('Mon, 13 Dec 2021 14:25:40 GMT')
  })

  it('should test hasBeenIssued', () => {
    expect(hasBeenIssued(1641388013)).to.be.true
  })

  it('should test hasExpired', () => {
    expect(hasExpired(1641388013)).to.be.true
  })
})
