package code.api.util.newstyle

import code.api.util.APIUtil.{OBPReturnType, unboxFullOrFail}
import code.api.util.ErrorMessages.{BankAccountBalanceNotFoundById, InvalidConnectorResponse}
import code.api.util.CallContext
import code.bankaccountbalance.BankAccountBalanceX
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model.{AccountId, BankAccountBalanceTrait, BankId}
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
            s"$BankAccountBalanceNotFoundById Current BALANCE_ID(${balanceId.value})",
            404),
          callContext
        )
    }
  }

  def createOrUpdateBankAccountBalance(
    bankId: BankId,
    accountId: AccountId,
    balanceId: Option[BalanceId],
    balanceType: String,
    balanceAmount: BigDecimal,
    callContext: Option[CallContext]
  ): OBPReturnType[BankAccountBalanceTrait] = {
    BankAccountBalanceX.bankAccountBalanceProvider.vend.createOrUpdateBankAccountBalance(
      bankId,
      accountId,
      balanceId,
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