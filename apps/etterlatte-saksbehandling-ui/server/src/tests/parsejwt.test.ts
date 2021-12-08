import { expect } from "chai";
import { parseJwt } from "../utils/parsejwt";
import { utcSecondsSinceEpoch } from "../utils/date";

const token =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Imwzc1EtNTBjQ0g0eEJWWkxIVEd3blNSNzY4MCJ9.eyJhdWQiOiI2NDZmZjZkOC00NmI4LTQyNWQtOGVlMS01OGJiYmEyNzY0N2IiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vNjIzNjY1MzQtMWVjMy00OTYyLTg4NjktOWI1NTM1Mjc5ZDBiL3YyLjAiLCJpYXQiOjE2Mzg4NzM5ODcsIm5iZiI6MTYzODg3Mzk4NywiZXhwIjoxNjM4ODc5NDE3LCJhaW8iOiJBVVFBdS84VEFBQUFXM3pHSmU2VXZFeWwwU2NCMGxBaGpjN0p3V29sc2JDOE9ESTJqa0VJTGZ0cVVzc2o5QzcvQkdzVW1sc3pkZEVlTmhhYjB4QXdNTmZWL0QxbGxXbkhPQT09IiwiYXpwIjoiNjQ2ZmY2ZDgtNDZiOC00MjVkLThlZTEtNThiYmJhMjc2NDdiIiwiYXpwYWNyIjoiMiIsImdyb3VwcyI6WyI2NTA2ODRmZi04MTA3LTRhZTQtOThmYy1lMThiNWNmMzE4OGIiXSwibmFtZSI6IldlZGluLCBHbGVubiIsIm9pZCI6ImM1ODBkN2ZlLWQ3NGUtNDE2OC05ZDE5LTZiNDU3MWYyYTZlOSIsInByZWZlcnJlZF91c2VybmFtZSI6IkdsZW5uLldlZGluQG5hdi5ubyIsInJoIjoiMC5BU0FBTkdVMllzTWVZa21JYVp0Vk5TZWRDOWoyYjJTNFJsMUNqdUZZdTdvblpIc2dBRWcuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6IjAxenQyb3ZWYmNrLUJQQ3Z4X1haMzc0REZKOXdHa2RubllLSHBfc1pOUzQiLCJ0aWQiOiI2MjM2NjUzNC0xZWMzLTQ5NjItODg2OS05YjU1MzUyNzlkMGIiLCJ1dGkiOiJla2xwUUlaR2pFU29aNERfZElkakFBIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJXMTYzMTQ2In0.kSu_ZdMTMtpvY8RHPOuXc1cHPATKK9jbq5ljMRhfLdcOOBdknMPrQBsnK5SB4rpAayP-tRJuZylhhtu6oyOgXiwevQitBoAXzWuQceI71ttH-N7dxnfRtd-6BNAp7huBU3x_dxeHeOOK63Fdd3sM2T1J66kKhk03TRCE2Z0fgM6JsPzDJkW1Sw3_b04_MKrCSk2NhhBJGlg1jsx1zNkwpATY_XCv8nRlNwgNwHnAAsy2ndAwmi8aZDPgdMO3bjsCr7998PoUPvbMnGDwKat_icmTJyXtEvcgXRILC2XcqlN_USHumBHvIBH9gqR1wT7m-ug44xfBA6F-DL5WGPCxZw";

describe("Test", () => {
    it("Should parse token successfully", () => {
        const parsed = parseJwt(token);
        expect(parsed).to.haveOwnProperty("aud");
        expect(parsed).to.haveOwnProperty("iss");
        expect(parsed).to.haveOwnProperty("exp");
        expect(parsed).to.haveOwnProperty("iat");
    });

    it("parse epoch to date", () => {
        const parsed = parseJwt(token);
        const date = new Date(0);
        date.setUTCSeconds(parsed.exp);
        expect(date.toUTCString()).to.equal("Tue, 07 Dec 2021 12:16:57 GMT");
    });

    it("should match date", () => {
      const parsed = parseJwt(token);
      console.log(parsed.exp);
      console.log(utcSecondsSinceEpoch())
    })
});
