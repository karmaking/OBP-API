package code.api.util.newstyle

import code.api.util.APIUtil.{OBPReturnType, unboxFullOrFail}
import code.api.util.ErrorMessages.{InvalidConnectorResponse}
import code.api.util.CallContext
import code.bankaccountbalance.{BankAccountBalanceX}
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model.{AccountId, BankAccountBalanceTrait}
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.model.BalanceId


object BankAccountBalanceNewStyle {

  def getBankAccountBalances(
    accountId: AccountId,
    callContext: Option[CallContext]
  ): OBPReturnType[List[BankAccountBalanceTrait]] = {
    BankAccountBalanceX.bankAccountBalanceProvider.vend.getBankAccountBalances(accountId).map {
      result =>
        (
          unboxFullOrFail(
            result,
            callContext,
            s"$InvalidConnectorResponse ${nameOf(getBankAccountBalances _)}",
            404),
          callContext
        )
    }
  }

  def getBankAccountBalanceById(
    balanceId: BalanceId,
    callContext: Option[CallContext]
  ): OBPReturnType[BankAccountBalanceTrait] = {
    BankAccountBalanceX.bankAccountBalanceProvider.vend.getBankAccountBalanceById(balanceId).map {
      result =>
        (
          unboxFullOrFail(
            result,
            callContext,
            s"$InvalidConnectorResponse ${nameOf(getBankAccountBalanceById _)}",
            404),
          callContext
        )
    }
  }

  def createOrUpdateBankAccountBalance(
    balanceId: Option[BalanceId],
    accountId: AccountId,
    balanceType: String,
    balanceAmount: BigDecimal,
    callContext: Option[CallContext]
  ): OBPReturnType[BankAccountBalanceTrait] = {
    BankAccountBalanceX.bankAccountBalanceProvider.vend.createOrUpdateBankAccountBalance(
      balanceId,
      accountId,
      balanceType,
      balanceAmount
    ).map {
      result =>
        (
          unboxFullOrFail(
            result,
            callContext,
            s"$InvalidConnectorResponse ${nameOf(createOrUpdateBankAccountBalance _)}",
            400),
          callContext
        )
    }
  }

  def deleteBankAccountBalance(
    balanceId: BalanceId,
    callContext: Option[CallContext]
  ): OBPReturnType[Boolean] = {
    BankAccountBalanceX.bankAccountBalanceProvider.vend.deleteBankAccountBalance(balanceId).map {
      result =>
        (
          unboxFullOrFail(
            result,
            callContext,
            s"$InvalidConnectorResponse ${nameOf(deleteBankAccountBalance _)}",
            400),
          callContext
        )
    }
  }
  
}