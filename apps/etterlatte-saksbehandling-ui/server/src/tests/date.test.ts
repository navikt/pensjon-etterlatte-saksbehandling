import { epochToUTC, utcSecondsSinceEpoch } from "../utils/date";
import { expect } from 'chai'

describe("Test date functions", () => {

  it("should test epochToUTC", () => {
    expect(epochToUTC(1639405540).toUTCString()).to.equal("Mon, 13 Dec 2021 14:25:40 GMT");
  });
  

})