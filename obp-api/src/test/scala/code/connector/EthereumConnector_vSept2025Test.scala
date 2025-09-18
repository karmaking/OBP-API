package code.connector

import code.api.util.ErrorMessages
import code.api.v5_1_0.V510ServerSetup
import code.bankconnectors.ethereum.EthereumConnector_vSept2025
import com.github.dwickern.macros.NameOf
import com.openbankproject.commons.model._
import net.liftweb.common.Full
import org.scalatest.Tag

import scala.concurrent.Await
import scala.concurrent.duration._
/**
  * Minimal unit test to invoke makePaymentv210 against local Anvil.
  * Assumptions:
  *  - ethereum.rpc.url points to http://127.0.0.1:8545
  *  - The RPC allows eth_sendTransaction (Anvil unlocked accounts)
  *  - We pass BankAccount stubs with accountId holding 0x addresses
  */
class EthereumConnector_vSept2025Test extends V510ServerSetup{

  object ConnectorTestTag extends Tag(NameOf.nameOfType[EthereumConnector_vSept2025Test])
  
  object StubConnector extends EthereumConnector_vSept2025

  private case class StubBankAccount(id: String) extends BankAccount {
    override val accountId: AccountId = AccountId(id)
    override val bankId: BankId = BankId("bank-x")
    override val accountType: String = "checking"
    override val balance: BigDecimal = BigDecimal(0)
    override val currency: String = "ETH"
    override val name: String = "stub"
    override val label: String = "stub"
    override val number: String = "stub"
    override val lastUpdate: java.util.Date = new java.util.Date()
    override val accountHolder: String = "stub"
    override val accountRoutings: List[AccountRouting] = Nil
    override def branchId: String = "stub"
    override def accountRules: List[AccountRule] = Nil
  }

  feature("Make sure connector follow the obp general rules ") {
    scenario("OutBound case class should have the same param name with connector method", ConnectorTestTag) {
      val from = StubBankAccount("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
      val to   = StubBankAccount("0x70997970C51812dc3A010C7d01b50e0d17dc79C8")
      val amount = BigDecimal("0.0001")

      val trxBody = new TransactionRequestCommonBodyJSON {
        override val value: AmountOfMoneyJsonV121 = AmountOfMoneyJsonV121("ETH", amount.toString)
        override val description: String = "test"
      }

      // This is only for testing; you can comment it out when the local Anvil is running.
      val resF = StubConnector.makePaymentv210(
        from,
        to,
        TransactionRequestId(java.util.UUID.randomUUID().toString),
        trxBody,
        amount,
        "test",
        TransactionRequestType("ETHEREUM") ,
        "none",
        None
      )

      val res = Await.result(resF, 10.seconds)
      res._1 shouldBe a [Full[_]]
      val txId = res._1.openOrThrowException(ErrorMessages.UnknownError)
      txId.value should startWith ("0x")
    }
  }
}


