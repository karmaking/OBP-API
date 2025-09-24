package code.bankconnectors.ethereum

import net.liftweb.json._
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

class DecodeRawTxTest extends FeatureSpec with Matchers with GivenWhenThen {

  feature("Decode raw Ethereum transaction to JSON") {
    scenario("Decode a legacy signed raw transaction successfully") {
      Given("a sample legacy signed raw transaction hex string")
      val rawTx = "0xf86b178203e882520894627306090abab3a6e1400e9345bc60c78a8bef57880de0b6b3a764000080820ff6a016878a008fb817df6d771749336fa0c905ec5b7fafcd043f0d9e609a2b5e41e0a0611dbe0f2ee2428360c72f4287a2996cb0d45cb8995cc23eb6ba525cb9580e02"

      When("we decode it to JSON string")
      val jsonStr = DecodeRawTx.decodeRawTxToJson(rawTx)

      Then("the JSON contains the expected basic fields")
      implicit val formats: Formats = DefaultFormats
      val jValue = parse(jsonStr)

      (jValue \ "hash").extract[String] should startWith ("0x")
      (jValue \ "type").extract[String] shouldBe "0"
      (jValue \ "nonce").extract[String] shouldBe "0x17"
      (jValue \ "gasPrice").extract[String] shouldBe "0x3e8"
      (jValue \ "gas").extract[String] shouldBe "0x5208"
      (jValue \ "to").extract[String].toLowerCase shouldBe "0x627306090abab3a6e1400e9345bc60c78a8bef57"
      (jValue \ "value").extract[String] shouldBe "0xde0b6b3a7640000"
      val inputData = (jValue \ "input").extract[String]
      inputData should (be ("0x") or be (""))

      And("signature fields are present")
      (jValue \ "v").extract[String] should startWith ("0x")
      (jValue \ "r").extract[String] should startWith ("0x")
      (jValue \ "s").extract[String] should startWith ("0x")

      And("estimatedFee exists and is hex with 0x prefix")
      (jValue \ "estimatedFee").extract[String] should startWith ("0x")
    }
  }
}


